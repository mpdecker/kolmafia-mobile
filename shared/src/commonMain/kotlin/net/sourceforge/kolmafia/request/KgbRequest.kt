package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for Kremlin's Greatest Briefcase place.php actions. */
open class KgbRequest(
    private val client: HttpClient,
    private val preferences: Preferences?,
    private val sessionLogger: SessionLogger?,
    private val refreshModifiers: (() -> Unit)? = null,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

    open suspend fun visit(): Result<String> = getPlace()

    open suspend fun button(action: String): Result<String> {
        val resolved = resolveButtonAction(action)
            ?: return Result.failure(IllegalArgumentException("Unknown KGB button: $action"))
        return getPlace(action = resolved)
    }

    open suspend fun dispenser(itemId: Int): Result<String> {
        if (itemId <= 0) {
            return Result.failure(IllegalArgumentException("KGB dispenser item ID must be positive."))
        }
        return getPlace(action = ACTION_DISPENSER, itemId = itemId, requireDispenserSuccess = true)
    }

    fun parseResponse(url: String, html: String): Boolean {
        val signature = url to html
        if (signature in handledSignatures) return true
        val handled = parseResponse(url, html, preferences, refreshModifiers)
        if (handled) handledSignatures += signature
        return handled
    }

    private suspend fun getPlace(
        action: String? = null,
        itemId: Int? = null,
        requireDispenserSuccess: Boolean = false,
    ): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/place.php") {
                parameter("whichplace", PLACE)
                if (!action.isNullOrBlank()) parameter("action", action)
                if (itemId != null) parameter("whichitem", itemId.toString())
            }
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("KGB request failed."))
            }
            val html = response.bodyAsText()
            val url = buildString {
                append("place.php?whichplace=").append(PLACE)
                if (!action.isNullOrBlank()) append("&action=").append(action)
                if (itemId != null) append("&whichitem=").append(itemId)
            }
            val handled = parseResponse(url, html)
            if (requireDispenserSuccess && !handled) {
                return Result.failure(IllegalStateException("KGB dispenser response was not successful."))
            }
            if (!action.isNullOrBlank()) {
                sessionLogger?.appendRawLine("kgb $action")
            }
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val PLACE = "kgb"
        const val ITEM_NAME = "Kremlin's Greatest Briefcase"
        const val ITEM_ID = 9493
        private const val ACTION_DISPENSER = "kgb_dispenser"
        private val ACTION_FIELD = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
        private val PLACE_FIELD = Regex("""(?:^|[?&])whichplace=([^&]+)""", RegexOption.IGNORE_CASE)
        private val ENCHANT_PATTERN = Regex("""<s>(.*?)</s><br><br><b>(.*?)</b>""", RegexOption.IGNORE_CASE)
        private val BUTTON_ACTION = Regex("""^(?:kgb_button)?(\d+)$""", RegexOption.IGNORE_CASE)

        fun isKgbUrl(url: String): Boolean =
            url.contains("place.php", ignoreCase = true) &&
                PLACE_FIELD.find(url)?.groupValues?.getOrNull(1).equals(PLACE, ignoreCase = true)

        fun parseResponse(
            url: String,
            html: String,
            preferences: Preferences?,
            refreshModifiers: (() -> Unit)? = null,
        ): Boolean {
            if (!isKgbUrl(url)) return false
            val action = ACTION_FIELD.find(url)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
            if (action.isEmpty()) return false

            countClicks(html, preferences)

            return when {
                action.startsWith("kgb_button") -> {
                    val changed = updateEnchantments(html)
                    if (changed) refreshModifiers?.invoke()
                    true
                }
                action == ACTION_DISPENSER -> parseDispenser(html, preferences)
                action == "kgb_drawer1" -> {
                    preferences?.setBoolean("_kgbRightDrawerUsed", true)
                    true
                }
                action == "kgb_drawer2" -> {
                    preferences?.setBoolean("_kgbLeftDrawerUsed", true)
                    true
                }
                action == "kgb_daily" -> {
                    preferences?.setBoolean("_kgbOpened", true)
                    true
                }
                action.startsWith("kgb_handle") -> {
                    if (html.contains("The case emanates warmth.")) {
                        preferences?.setBoolean("_kgbFlywheelCharged", true)
                    }
                    true
                }
                else -> false
            }
        }

        private fun parseDispenser(html: String, preferences: Preferences?): Boolean {
            when {
                html.contains("You acquire an item", ignoreCase = true) -> {
                    val uses = (preferences?.getInt("_kgbDispenserUses", 0) ?: 0) + 1
                    preferences?.setInt("_kgbDispenserUses", uses)
                    return true
                }
                html.contains("out of juice", ignoreCase = true) -> {
                    preferences?.setInt("_kgbDispenserUses", 3)
                    return true
                }
                else -> return false
            }
        }

        private fun countClicks(html: String, preferences: Preferences?) {
            val startIndex = html.indexOf("<br>Click", ignoreCase = true)
            if (startIndex < 0) return
            val textStart = startIndex + 4
            val endIndex = html.indexOf("<br>", textStart, ignoreCase = true)
            if (endIndex < 0) return
            val text = html.substring(textStart, endIndex).lowercase()
            var count = 0
            var index = text.indexOf("click")
            while (index >= 0) {
                count++
                index = text.indexOf("click", index + 5)
            }
            if (count <= 0) return
            val used = (preferences?.getInt("_kgbClicksUsed", 0) ?: 0) + count
            preferences?.setInt("_kgbClicksUsed", used)
        }

        private fun updateEnchantments(html: String): Boolean {
            if (!html.contains("symphony of mechanical", ignoreCase = true)) return false
            val match = ENCHANT_PATTERN.find(html) ?: return false
            val oldEnchantment = match.groupValues[1]
            val newEnchantment = match.groupValues[2]
            val oldMods = ENCHANTMENTS[oldEnchantment] ?: return false
            val newMods = ENCHANTMENTS[newEnchantment] ?: return false
            val current = ModifierDatabase.getItem(ITEM_NAME)?.modifiers.orEmpty()
            val oldTags = oldMods.map { modifierTag(it) }.toSet()
            val kept = current.split(',').map { it.trim() }.filter { token ->
                token.isNotEmpty() && modifierTag(token) !in oldTags
            }
            ModifierDatabase.overrideModifier("Item", ITEM_NAME, (kept + newMods).joinToString(", "))
            return true
        }

        private fun modifierTag(token: String): String {
            val colon = token.indexOf(':')
            return if (colon < 0) token.trim() else token.substring(0, colon).trim()
        }

        private fun resolveButtonAction(action: String): String? {
            val trimmed = action.trim()
            if (trimmed.startsWith("kgb_button", ignoreCase = true)) return trimmed.lowercase()
            val numbered = BUTTON_ACTION.matchEntire(trimmed) ?: return null
            return "kgb_button${numbered.groupValues[1]}"
        }

        private val ENCHANTMENTS = mapOf(
            "Weapon Damage +25%" to listOf("Weapon Damage Percent: +25"),
            "Spell Damage +50%" to listOf("Spell Damage Percent: +50"),
            "+5 Prismatic Damage" to listOf(
                "Hot Damage: +5",
                "Cold Damage: +5",
                "Spooky Damage: +5",
                "Stench Damage: +5",
                "Sleaze Damage: +5",
            ),
            "+10% chance of Critical Hit" to listOf("Critical Hit Percent: +10"),
            "+5 PvP Fights per day" to listOf("PvP Fights: +5"),
            "Monsters will be less attracted to you" to listOf("Combat Rate: -5"),
            "Monsters will be more attracted to you" to listOf("Combat Rate: +5"),
            "+25 to Monster Level" to listOf("Monster Level: +25"),
            "-3 MP to use Skills" to listOf("Mana Cost: -3"),
            "Regenerate 5-10 HP & MP per Adventure" to listOf(
                "HP Regen Min: 5",
                "HP Regen Max: 10",
                "MP Regen Min: 5",
                "MP Regen Max: 10",
            ),
            "+5 Adventures per day" to listOf("Adventures: +5"),
            "+25% Combat Initiative" to listOf("Initiative: +25"),
            "Damage Absorption +100" to listOf("Damage Absorption: +100"),
            "Superhuman Hot Resistance (+5)" to listOf("Hot Resistance: +5"),
            "Superhuman Cold Resistance (+5)" to listOf("Cold Resistance: +5"),
            "Superhuman Spooky Resistance (+5)" to listOf("Spooky Resistance: +5"),
            "Superhuman Stench Resistance (+5)" to listOf("Stench Resistance: +5"),
            "Superhuman Sleaze Resistance (+5)" to listOf("Sleaze Resistance: +5"),
        )
    }
}
