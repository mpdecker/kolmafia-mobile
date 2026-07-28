package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.request.StandardRequest

/** Desktop [ItemDatabase.isAllowed] for physical accessible counts. */
object ItemRestriction {

    fun isAllowed(
        itemId: Int,
        itemName: String,
        characterState: CharacterState?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (itemId < 1) return true
        val dataName = gameDatabase?.item(itemId)?.name
            ?: ItemDatabase.getById(itemId)?.name
            ?: itemName
        return StandardRequest.isAllowed(RestrictedItemType.ITEMS, dataName, characterState)
    }
}
