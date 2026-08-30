package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Headless town_right.php Gourd Captain request and response parser. */
class GourdRequest(
    private val client: HttpClient,
    private val preferences: Preferences,
    private val character: KoLCharacter,
    private val inventory: InventoryManager,
    private val sessionLogger: SessionLogger? = null,
) {
    suspend fun visit(): Result<String> = request("town_right.php?place=gourd")

    suspend fun acceptQuest(): Result<String> = request("town_right.php?action=acceptgourdquest")

    suspend fun trade(): Result<String> {
        val count = preferences.getInt("gourdItemCount", 5)
        val itemId = gourdItemId(character.state.value.characterClassEnum.mainStat)
        preferences.setInt("gourdItemId", itemId)
        val available = inventory.state.value.items[itemId]?.quantity ?: 0
        if (available < count) return Result.failure(IllegalStateException("Need $count gourd items."))
        return request("town_right.php?action=gourd")
    }

    private suspend fun request(path: String): Result<String> = try {
        registerRequest(path, preferences, character, inventory, sessionLogger)
        Result.success(client.get("$KOL_BASE_URL/$path").bodyAsText().also {
            parseResponse(path, it, preferences, inventory)
        })
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val REQUIRED = Regex("""Bring back\s+(\d+)""", RegexOption.IGNORE_CASE)
        private val URP = Regex("""The\s+(\d+)\s+<i>urp</i>""", RegexOption.IGNORE_CASE)
        private val GIVE = Regex("""value=["']Give him\s+(\d+)""", RegexOption.IGNORE_CASE)

        fun gourdItemId(mainStat: MainStat): Int = when (mainStat) {
            MainStat.MUSCLE -> ItemPool.KNOB_FIRECRACKER
            MainStat.MYSTICALITY -> ItemPool.CAN_LID
            MainStat.MOXIE -> ItemPool.SPIDER_WEB
        }

        fun parseResponse(
            location: String?,
            responseText: String?,
            preferences: Preferences?,
            inventory: InventoryManager? = null,
        ): Boolean {
            if (location == null || responseText == null || !location.startsWith("town_right.php")) return false
            if (location.contains("acceptgourdquest")) {
                preferences?.setInt("gourdItemCount", 5)
                preferences?.setString("questM06Gourd", "started")
                return true
            }
            if (location.contains("action=gourd")) {
                if (!responseText.contains("You acquire", ignoreCase = true)) return false
                val count = preferences?.getInt("gourdItemCount", 5) ?: 5
                val itemId = preferences?.getInt("gourdItemId", 0)?.takeIf { it > 0 }
                itemId?.let { inventory?.consumeItemLocally(it, count) }
                preferences?.setInt("gourdItemCount", count + 1)
                return true
            }
            if (!location.contains("place=gourd")) return false
            val count = REQUIRED.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
                ?: URP.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
                ?: GIVE.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
                ?: 26
            preferences?.setInt("gourdItemCount", count)
            return true
        }

        fun registerRequest(
            url: String,
            preferences: Preferences?,
            character: KoLCharacter? = null,
            inventory: InventoryManager? = null,
            logger: SessionLogger? = null,
        ): Boolean {
            if (!url.startsWith("town_right.php")) return false
            val message = when {
                url.contains("action=gourd") -> {
                    val count = preferences?.getInt("gourdItemCount", 5) ?: 5
                    val itemId = preferences?.getInt("gourdItemId", 0)?.takeIf { it > 0 }
                        ?: character?.let { gourdItemId(it.state.value.characterClassEnum.mainStat) }
                    val name = itemId?.let { net.sourceforge.kolmafia.data.ItemDatabase.getItemName(it) } ?: "gourd item"
                    if (itemId != null && inventory?.state?.value?.items?.get(itemId)?.quantity ?: 0 < count) return true
                    "Giving $count $name(s) to the Captain of the Gourd"
                }
                url.contains("acceptgourdquest") -> "Accepting the Gourd Quest"
                url.contains("place=gourd") -> "Visiting the Captain of the Gourd"
                else -> return false
            }
            logger?.appendRawLine(message)
            return true
        }
    }
}
