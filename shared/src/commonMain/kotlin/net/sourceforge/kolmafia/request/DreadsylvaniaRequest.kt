package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop `DreadsylvaniaRequest` — clan_dreadsylvania.php hub.
 *
 * Covers the Dreadsylvanian map shortcut discovery, zone routing via `forceloc`,
 * and booze feeding to the coachman via `feedbooze`.
 */
object DreadsylvaniaRequest {

    // Desktop SHORTCUTS: [name, zone, image, setting]
    private data class Shortcut(
        val name: String,
        val zone: String,
        val image: String,
        val setting: String,
    )

    private val SHORTCUTS = listOf(
        Shortcut("The Cabin", "Dreadsylvanian Woods", "shortcut1.gif", "ghostPencil1"),
        Shortcut("The Tallest Tree", "Dreadsylvanian Woods", "shortcut2.gif", "ghostPencil2"),
        Shortcut("The Burrows", "Dreadsylvanian Woods", "shortcut3.gif", "ghostPencil3"),
        Shortcut("The Village Square", "Dreadsylvanian Village", "shortcut4.gif", "ghostPencil4"),
        Shortcut("Skid Row", "Dreadsylvanian Village", "shortcut5.gif", "ghostPencil5"),
        Shortcut("The Old Duke's Estate", "Dreadsylvanian Village", "shortcut6.gif", "ghostPencil6"),
        Shortcut("The Great Hall", "Dreadsylvanian Castle", "shortcut7.gif", "ghostPencil7"),
        Shortcut("The Tower", "Dreadsylvanian Castle", "shortcut8.gif", "ghostPencil8"),
        Shortcut("The Dungeons", "Dreadsylvanian Castle", "shortcut9.gif", "ghostPencil9"),
    )

    private val LOC_PATTERN = Regex("""loc=(\d+)""", RegexOption.IGNORE_CASE)
    private val WHICHBOOZE_PATTERN = Regex("""whichbooze=(\d+)""", RegexOption.IGNORE_CASE)
    private val BOOZEQUANTITY_PATTERN = Regex("""boozequantity=(\d+)""", RegexOption.IGNORE_CASE)
    private val SHORTCUT_PATTERN = Regex("""otherimages/dv/(shortcut\d\.gif)""")
    private val ACTION_PATTERN = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)

    // ── public shortcut catalog helpers ──────────────────────────────────────

    fun shortcutImageToIndex(image: String): Int =
        SHORTCUTS.indexOfFirst { it.image == image }

    fun shortcutIndexToName(index: Int): String? =
        SHORTCUTS.getOrNull(index)?.name

    fun shortcutIndexToZone(index: Int): String? =
        SHORTCUTS.getOrNull(index)?.zone

    fun shortcutIndexToImage(index: Int): String? =
        SHORTCUTS.getOrNull(index)?.image

    fun shortcutIndexToSetting(index: Int): String? =
        SHORTCUTS.getOrNull(index)?.setting

    // ── URL parsing helpers ─────────────────────────────────────────────────

    private fun getAction(url: String): String? =
        ACTION_PATTERN.find(url)?.groupValues?.get(1)

    /** Map `loc=N` (1-based) to the adventure zone name. */
    fun getAdventureZone(url: String): String? {
        val loc = LOC_PATTERN.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return shortcutIndexToZone(loc - 1)
    }

    /** Extract booze item ID and quantity from a feedbooze URL. Returns (itemId, count) or null. */
    private fun getBooze(url: String): Pair<Int, Int>? {
        val itemId = WHICHBOOZE_PATTERN.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val count = BOOZEQUANTITY_PATTERN.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        return itemId to count
    }

    // ── response parsing ────────────────────────────────────────────────────

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ) {
        if (preferences == null) return
        if (!url.contains("clan_dreadsylvania.php", ignoreCase = true)) return

        val action = getAction(url)
        if (action == null) {
            // Parse the map — discover shortcuts
            val matches = SHORTCUT_PATTERN.findAll(html)
            for (match in matches) {
                val index = shortcutImageToIndex(match.groupValues[1])
                if (index == -1) continue
                val setting = shortcutIndexToSetting(index) ?: continue
                if (!preferences.getBoolean(setting, false)) {
                    preferences.setBoolean(setting, true)
                }
            }
            return
        }

        if (action.equals("feedbooze", ignoreCase = true)) {
            val (itemId, count) = getBooze(url) ?: return
            preferences.setInt("_dreadLastBooze", itemId)
            preferences.setInt("_dreadLastBoozeQty", count)
            ResultProcessor.processItem(itemId, -count, preferences, inventory = inventory)
        }
    }

    // ── request registration (session-log) ──────────────────────────────────

    /**
     * Desktop `DreadsylvaniaRequest.registerRequest` — session-log lines matching desktop text.
     *
     * - `forceloc`: sets LAST_LOCATION to the adventure zone (no log — redirects to adventure.php)
     * - `feedbooze`: logs "Feeding N {name} to the coachman"
     */
    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (!url.contains("clan_dreadsylvania.php", ignoreCase = true)) return false

        val action = getAction(url)
        if (action == null) {
            // Bare map visit — silently claimed
            return false
        }

        if (action.equals("forceloc", ignoreCase = true)) {
            val zone = getAdventureZone(url) ?: return false
            preferences?.setString(Preferences.LAST_LOCATION, zone)
            // Desktop: "Don't need to log this: it will redirect to adventure.php"
            return true
        }

        if (action.equals("feedbooze", ignoreCase = true)) {
            val (itemId, count) = getBooze(url) ?: return false
            val item = ItemDatabase.getById(itemId)
            val name = if (count == 1) {
                item?.name ?: "item #$itemId"
            } else {
                pluralName(itemId, count)
            }
            val message = "Feeding $count $name to the coachman"
            sessionLogger?.appendRawLine("")
            sessionLogger?.appendRawLine(message)
            return true
        }

        return false
    }

    fun getAdventuresUsed(url: String): Int =
        if (url.contains("action=adventure", ignoreCase = true) ||
            LOC_PATTERN.containsMatchIn(url)
        ) 1 else 0

    // ── internal helpers ────────────────────────────────────────────────────

    private fun pluralName(itemId: Int, count: Int): String {
        val item = ItemDatabase.getById(itemId) ?: return "$count item #$itemId"
        val plural = item.plural?.takeIf { it.isNotBlank() } ?: "${item.name}s"
        return plural
    }
}
