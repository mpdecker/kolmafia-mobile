package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [UseItemRequest.parseRobortenderBinge]. A successful
 * `inventory.php?action=robooze` page (text contains "the cocktail" or "the drink")
 * appends the drink name to `_roboDrinks` and consumes one of the item.
 * Anything else leaves the pref and the item alone and returns false.
 */
object UseItemRobortenderSync {
    const val PREF = "_roboDrinks"
    private val WHICHITEM = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)

    fun parse(
        url: String,
        responseText: String,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        itemName: (Int) -> String = { id ->
            ItemDatabase.getItemName(id).ifBlank { "item #$id" }
        },
    ): Boolean {
        val path = pagePath(url)
        if (!path.startsWith("inventory.php") || !path.contains("action=robooze")) return false
        if (!responseText.contains("the cocktail") && !responseText.contains("the drink")) return false
        val itemId = WHICHITEM.find(path)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        if (preferences != null) {
            val prior = preferences.getString(PREF, "")
            val name = itemName(itemId)
            preferences.setString(PREF, if (prior.isEmpty()) name else "$prior,$name")
        }
        inventory?.consumeItemLocally(itemId, 1)
        return true
    }

    private fun pagePath(url: String): String {
        val noHash = url.substringBefore('#')
        return if (noHash.contains("://")) {
            noHash.substringAfter("://").substringAfter('/')
        } else {
            noHash.trimStart('/')
        }
    }
}
