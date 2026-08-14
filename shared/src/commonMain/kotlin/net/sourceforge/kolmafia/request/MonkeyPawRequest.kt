package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop MonkeyPawRequest — main.php?action=cmonk → choice 1501. */
class MonkeyPawRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val equipmentRequest: EquipmentRequest? = null,
) {
    suspend fun makeWish(
        wish: String,
        preferences: Preferences?,
        charState: CharacterState?,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        val normalized = wish.trim()
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("No monkey paw wish specified."))
        }
        val preflight = preflightError(preferences, charState, inventoryCounts)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }
        val pawName = ItemDatabase.getItemName(PAW_ITEM_ID)
        val equipped = charState?.equipment?.values?.any {
            it.equals(pawName, ignoreCase = true)
        } == true
        if (!equipped && equipmentRequest != null && inventoryCounts(PAW_ITEM_ID) > 0) {
            equipmentRequest.equipItem(PAW_ITEM_ID, EquipmentSlot.ACC1)
                .onFailure { /* continue; KoL may still open from inventory */ }
        }
        return try {
            val visit = client.get("$KOL_BASE_URL/main.php?action=cmonk")
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Could not open cursed monkey paw."))
            }
            visitChoice(visit.bodyAsText(), preferences)
            val result = choiceRequest.choose(
                CHOICE_ID,
                1,
                mapOf("wish" to normalized),
            )
            result.map { (html, _) ->
                postChoice(html, preferences)
                html
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CHOICE_ID = 1501
        const val PAW_ITEM_ID = 11186
        const val WISHES_USED_PREF = "_monkeyPawWishesUsed"
        private val DISALLOWED = Regex("""[^a-zA-Z\d \-]""")
        private val FINGER_PATTERN =
            Regex("""It has (\d) fingers? held up expectantly\.""", RegexOption.IGNORE_CASE)
        private val CURSE_PATTERN =
            Regex(
                """<b>Cursed by a Monkey</b>.*?\(duration: (\d+) Adventures\)""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            )

        fun resolveWish(parameters: String): Result<String> {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Usage: monkeypaw effect|item|wish …"),
                )
            }
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("wish ") -> Result.success(trimmed.substring(5).trim())
                lower.startsWith("effect ") -> {
                    val name = trimmed.substring(7).trim()
                    val effect = EffectDatabase.getByName(name)
                        ?: return Result.failure(
                            IllegalArgumentException("$name does not match exactly one effect"),
                        )
                    val sub = getValidEffectSubstring(effect.name)
                        ?: return Result.failure(
                            IllegalArgumentException(
                                "cannot find unique valid substring to wish for ${effect.name}",
                            ),
                        )
                    Result.success(sub)
                }
                lower.startsWith("item ") -> {
                    val name = trimmed.substring(5).trim()
                    val item = ItemDatabase.getByName(name)
                        ?: return Result.failure(
                            IllegalArgumentException("$name does not match exactly one item"),
                        )
                    val sub = getValidItemSubstring(item.name)
                        ?: return Result.failure(
                            IllegalArgumentException(
                                "cannot find unique valid substring to wish for ${item.name}",
                            ),
                        )
                    Result.success(sub)
                }
                else -> Result.failure(
                    IllegalArgumentException("Usage: monkeypaw effect|item|wish …"),
                )
            }
        }

        fun getValidEffectSubstring(name: String): String? {
            val split = DISALLOWED.split(name).filter { it.isNotBlank() }
            if (split.size <= 1) return name
            for (entry in split) {
                if (matchingEffectNames(entry).size == 1) return entry
            }
            return null
        }

        fun getValidItemSubstring(name: String): String? {
            val split = DISALLOWED.split(name).filter { it.isNotBlank() }
            if (split.size <= 1) return name
            for (entry in split) {
                if (matchingItemNames(entry).size == 1) return entry
            }
            return null
        }

        fun matchingEffectNames(substring: String): List<String> {
            val key = substring.lowercase()
            if (key.isEmpty()) return emptyList()
            return EffectDatabase.all()
                .map { it.name }
                .filter { it.lowercase().contains(key) }
        }

        fun matchingItemNames(substring: String): List<String> {
            val key = substring.lowercase()
            if (key.isEmpty()) return emptyList()
            return ItemDatabase.all()
                .map { it.name }
                .filter { it.lowercase().contains(key) }
        }

        fun preflightError(
            preferences: Preferences?,
            charState: CharacterState?,
            inventoryCounts: (Int) -> Int,
        ): String? {
            val pawName = ItemDatabase.getItemName(PAW_ITEM_ID)
            val equipped = charState?.equipment?.values?.any {
                it.equals(pawName, ignoreCase = true)
            } == true
            if (!equipped && inventoryCounts(PAW_ITEM_ID) <= 0) {
                return "You do not have a cursed monkey paw."
            }
            val used = preferences?.getInt(WISHES_USED_PREF, 0) ?: 0
            if (used >= 5) {
                return "You have been cursed enough today."
            }
            return null
        }

        fun visitChoice(html: String, preferences: Preferences?) {
            if (preferences == null) return
            if (html.contains("It is closed in a tight withholding fist.")) {
                preferences.setInt(WISHES_USED_PREF, 5)
                return
            }
            val match = FINGER_PATTERN.find(html) ?: return
            val left = match.groupValues[1].toIntOrNull() ?: return
            preferences.setInt(WISHES_USED_PREF, 5 - left)
        }

        fun postChoice(html: String, preferences: Preferences?) {
            if (preferences == null) return
            if (!html.contains("Wish granted.")) return
            preferences.setInt(
                WISHES_USED_PREF,
                preferences.getInt(WISHES_USED_PREF, 0) + 1,
            )
            val curse = CURSE_PATTERN.find(html)
            if (curse != null) {
                val duration = curse.groupValues[1].toIntOrNull() ?: return
                preferences.setInt(WISHES_USED_PREF, duration / 7)
            }
        }
    }
}
