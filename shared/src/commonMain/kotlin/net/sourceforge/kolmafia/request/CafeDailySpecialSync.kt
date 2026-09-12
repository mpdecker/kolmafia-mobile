package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop MicroBreweryRequest / ChezSnooteeRequest cafe visit daily-special parse.
 * Pattern: Today's Special: … whichitem=ID … descitem("DESC") … NAME (PRICE Meat)
 */
object CafeDailySpecialSync {

    private val SPECIAL_PATTERN = Regex(
        """Today's Special:.*?name=whichitem value=(\d+).*?onclick='descitem\("(\d+)".*?<td>(.*?)\s*\(([\d,]+)\s*Meat\)</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /**
     * Parse cafe.php visit HTML and write `_dailySpecial` / `_dailySpecialPrice`.
     * @return resolved item name when a special was found
     */
    fun parseResponse(urlString: String, responseText: String, preferences: Preferences?): String? {
        if (!urlString.contains("cafe.php", ignoreCase = true)) return null
        if (urlString.contains("action=CONSUME", ignoreCase = true)) return null
        val cafeId = Regex("""[?&]cafeid=(\d+)""", RegexOption.IGNORE_CASE)
            .find(urlString)?.groupValues?.get(1)
        // Microbrewery=2, Chez Snootée=1 — both carry Today's Special
        if (cafeId != null && cafeId != "1" && cafeId != "2") return null
        val match = SPECIAL_PATTERN.find(responseText) ?: return null
        val itemId = match.groupValues[1].toIntOrNull() ?: return null
        val descId = match.groupValues[2]
        val itemName = match.groupValues[3].trim()
        val price = match.groupValues[4].replace(",", "").toIntOrNull() ?: 0
        var resolved = ItemDatabase.getItemName(itemId).takeIf { it.isNotBlank() }
            ?: ItemDatabase.getById(itemId)?.name
        if (resolved == null || !resolved.equals(itemName, ignoreCase = true)) {
            ItemDatabase.registerItem(itemId, itemName, descId)
            resolved = ItemDatabase.getItemName(itemId).takeIf { it.isNotBlank() } ?: itemName
        }
        preferences?.setString("_dailySpecial", resolved)
        preferences?.setInt("_dailySpecialItemId", itemId)
        if (price > 0) preferences?.setInt("_dailySpecialPrice", price)
        return resolved
    }

    fun currentSpecialName(preferences: Preferences?): String? =
        preferences?.getString("_dailySpecial", "")?.takeIf { it.isNotBlank() }
            ?: preferences?.getString("dailySpecial", "")?.takeIf { it.isNotBlank() }

    /** Desktop cafe special AdventureResult item id when known. */
    fun currentSpecialItemId(preferences: Preferences?): Int? =
        preferences?.getInt("_dailySpecialItemId", 0)?.takeIf { it > 0 }
}
