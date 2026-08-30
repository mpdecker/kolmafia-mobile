package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop AutoSellRequest.parseCompact / parseDetailed (Phases 2286–2300).
 */
object AutosellSync {
    private val ITEMID = Regex("""whichitem(?:\[\])?=(\d+)""", RegexOption.IGNORE_CASE)
    private val HOWMANY = Regex("""howmany(?:\[\]|=)(\d+)""", RegexOption.IGNORE_CASE)
    private val QUANTITY = Regex("""quantity(?:\[\]|=)(\d+)""", RegexOption.IGNORE_CASE)
    private val YOU_GAIN_MEAT = Regex("""You gain ([\d,]+) Meat""", RegexOption.IGNORE_CASE)

    fun parseCompact(
        url: String,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ): Boolean {
        if (!url.contains("sellstuff.php", ignoreCase = true)) return false
        val itemIds = ITEMID.findAll(url).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (itemIds.isEmpty()) return false
        val requested = HOWMANY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: QUANTITY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        var total = 0L
        itemIds.forEach { itemId ->
            val inventoryCount = inventory?.state?.value?.items?.get(itemId)?.quantity ?: requested
            val qty = when {
                url.contains("type=allbutone", true) -> (inventoryCount - 1).coerceAtLeast(0)
                url.contains("type=all", true) -> inventoryCount
                else -> requested
            }
            inventory?.consumeItemLocally(itemId, qty)
            total += (ItemDatabase.getById(itemId)?.autosellPrice ?: 0).toLong() * qty
        }
        if (total > 0 && character != null) {
            ResultProcessor.processMeat(total, character)
        }
        return true
    }

    fun parseDetailed(
        url: String,
        html: String,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ): Boolean {
        if (!url.contains("sellstuff", ignoreCase = true)) return false
        val itemIds = ITEMID.findAll(url).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        val qty = HOWMANY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: QUANTITY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        if (itemIds.isNotEmpty()) {
            itemIds.forEach { inventory?.consumeItemLocally(it, qty) }
        }
        val meat = YOU_GAIN_MEAT.find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
        if (meat != null && meat > 0) {
            ResultProcessor.processMeat(meat, character)
        } else if (itemIds.isNotEmpty()) {
            val fallback = itemIds.sumOf { (ItemDatabase.getById(it)?.autosellPrice ?: 0).toLong() * qty }
            if (fallback > 0) ResultProcessor.processMeat(fallback, character)
        }
        return itemIds.isNotEmpty() || meat != null
    }
}
