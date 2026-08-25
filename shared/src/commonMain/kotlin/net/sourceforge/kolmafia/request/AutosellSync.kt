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
    private val ITEMID = Regex("""whichitem(?:\[\]|=)(\d+)""", RegexOption.IGNORE_CASE)
    private val HOWMANY = Regex("""howmany(?:\[\]|=)(\d+)""", RegexOption.IGNORE_CASE)
    private val QUANTITY = Regex("""quantity(?:\[\]|=)(\d+)""", RegexOption.IGNORE_CASE)
    private val YOU_GAIN_MEAT = Regex("""You gain ([\d,]+) Meat""", RegexOption.IGNORE_CASE)

    fun parseCompact(
        url: String,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ): Boolean {
        if (!url.contains("sellstuff.php", ignoreCase = true)) return false
        val itemId = ITEMID.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        val qty = HOWMANY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: QUANTITY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        val price = ItemDatabase.getById(itemId)?.autosellPrice ?: 0
        inventory?.consumeItemLocally(itemId, qty)
        if (price > 0 && character != null) {
            ResultProcessor.processMeat(price.toLong() * qty, character)
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
        val itemId = ITEMID.find(url)?.groupValues?.get(1)?.toIntOrNull()
        val qty = HOWMANY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: QUANTITY.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        if (itemId != null && itemId > 0) {
            inventory?.consumeItemLocally(itemId, qty)
        }
        val meat = YOU_GAIN_MEAT.find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
        if (meat != null && meat > 0) {
            ResultProcessor.processMeat(meat, character)
        } else if (itemId != null && itemId > 0) {
            val price = ItemDatabase.getById(itemId)?.autosellPrice ?: 0
            if (price > 0) ResultProcessor.processMeat(price.toLong() * qty, character)
        }
        return itemId != null || meat != null
    }
}
