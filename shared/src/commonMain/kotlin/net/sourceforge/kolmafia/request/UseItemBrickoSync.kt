package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager

/**
 * Desktop [UseItemRequest.parseBricko]. “You break apart your …” consumes one
 * of the named bricko. Any other page leaves inventory alone.
 */
object UseItemBrickoSync {
    private val BROKEN = Regex("""You break apart your ([\w\s]*).""")

    fun parse(
        responseText: String,
        inventory: InventoryManager? = null,
        itemIdForName: (String) -> Int? = { name ->
            ItemDatabase.getByName(name)?.id ?: ItemDatabase.getByPluralOrName(name)?.id
        },
    ) {
        if (!responseText.contains("You break apart your")) return
        val name = BROKEN.find(responseText)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (name.isEmpty()) return
        val itemId = itemIdForName(name) ?: return
        inventory?.consumeItemLocally(itemId, 1)
    }
}
