package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/**
 * Desktop MallSearchRequest.updateSearchString / getSearchString preflight.
 * Returns null when the mall HTTP search should be skipped (NPC/coinmaster-only).
 */
object MallSearchPreflight {

    data class Result(
        val searchString: String,
        val skipMallHttp: Boolean,
        val matchedNames: List<String>,
    )

    fun getSearchString(itemName: String): String {
        val item = ItemDatabase.getByName(itemName.trim().trim('"'))
            ?: ItemDatabase.getMatchingNames(itemName).firstOrNull()?.let { ItemDatabase.getByName(it) }
        val dataName = item?.name ?: return itemName
        return if (dataName.contains('&')) decodeEntities(dataName) else dataName
    }

    fun updateSearchString(raw: String): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result(searchString = "", skipMallHttp = false, matchedNames = emptyList())
        }
        val exact = trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2
        val matches = ItemDatabase.getMatchingNames(trimmed).toMutableList()
        if (matches.isEmpty()) {
            // Unknown exact search still goes to mall; fuzzy unknown skips nothing when exact.
            return Result(searchString = trimmed, skipMallHttp = false, matchedNames = emptyList())
        }

        var npcItemCount = 0
        var untradeableCount = 0
        val retained = mutableListOf<String>()
        for (name in matches) {
            val itemId = ItemDatabase.getByName(name)?.id ?: continue
            val untradeable = !ItemDatabase.isTradeable(itemId)
            val npcOrCoin =
                NpcStoreDatabase.containsItem(itemId, validate = false) ||
                    CoinmasterDatabase.containsBuyItem(itemId, validate = false)
            if (npcOrCoin) {
                npcItemCount++
                if (untradeable) untradeableCount++
                retained += name
            } else if (!untradeable) {
                retained += name
            } else {
                // Drop untradeable non-NPC/coinmaster hits (desktop iterator.remove).
            }
        }
        matches.clear()
        matches.addAll(retained)

        val count = matches.size
        if (count == 0) {
            return Result(searchString = trimmed, skipMallHttp = false, matchedNames = emptyList())
        }
        if (count == untradeableCount) {
            return Result(searchString = trimmed, skipMallHttp = true, matchedNames = matches.toList())
        }
        var search = trimmed
        if (count == 1 && !exact) {
            search = "\"${getSearchString(matches.first())}\""
        }
        return Result(searchString = search, skipMallHttp = false, matchedNames = matches.toList())
    }

    fun decodeEntities(text: String): String {
        var s = text
        s = s.replace("&amp;", "&")
        s = s.replace("&lt;", "<")
        s = s.replace("&gt;", ">")
        s = s.replace("&quot;", "\"")
        s = s.replace("&apos;", "'")
        s = s.replace("&nbsp;", " ")
        s = NUMERIC_ENTITY.replace(s) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
        s = HEX_ENTITY.replace(s) { m ->
            m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
        }
        return s
    }

    private val NUMERIC_ENTITY = Regex("""&#(\d+);""")
    private val HEX_ENTITY = Regex("""&#x([0-9a-fA-F]+);""")
}
