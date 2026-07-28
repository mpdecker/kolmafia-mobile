package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.request.ThriftyRequest

/** Desktop [InventoryManager.pullableInLoL] / [pullableInSeaPath] / storage pull gates. */
object PullableItems {

    const val MAYAM_CALENDAR_ID = 11572

    private val SEA_NON_PULLABLE_IDS = setOf(
        3487, // rough fish scale
        3488, // pristine fish scale
        3602, // rusty broken diving helmet
        3607, // aerated diving helmet
        3699, // teflon ore
        3712, // teflon swim fins
        4197, // sea leather
        4199, // sea cowboy hat
        4281, // merkin bunwig
        4282, // crappy mask
        4283, // crappy tailpiece
        4284, // gladiator mask
        4285, // scholar mask
        4286, // gladiator tailpiece
        4287, // scholar tailpiece
        4288, // merkin headguard
        4289, // merkin waistrope
        4290, // merkin facecowl
        4291, // merkin thighguard
        4292, // merkin dodgeball
        4293, // merkin dragnet
        4294, // merkin switchblade
        6317, // sea chaps
        11976, // unblemished pearl
    )

    fun pullableInLoL(itemId: Int, item: ItemData?): Boolean {
        if (itemId == MAYAM_CALENDAR_ID) return false
        val data = item ?: return false
        return when (data.primaryUse) {
            ItemPrimaryUse.FOOD,
            ItemPrimaryUse.DRINK,
            ItemPrimaryUse.POTION,
            ItemPrimaryUse.AVATAR,
            ItemPrimaryUse.USABLE,
            ItemPrimaryUse.MULTIPLE,
            ItemPrimaryUse.REUSABLE,
            ItemPrimaryUse.MESSAGE,
            ItemPrimaryUse.FOOD_HELPER,
            ItemPrimaryUse.DRINK_HELPER,
            ItemPrimaryUse.CARD,
            ItemPrimaryUse.FOLDER,
            ItemPrimaryUse.BOOTSKIN,
            ItemPrimaryUse.BOOTSPUR,
            -> true
            ItemPrimaryUse.NONE ->
                data.secondaryUses.any { it.contains("combat", ignoreCase = true) }
            else -> false
        }
    }

    fun pullableInSeaPath(itemId: Int): Boolean = itemId !in SEA_NON_PULLABLE_IDS

    fun storagePullAllowed(
        characterState: CharacterState?,
        itemId: Int,
        gameDatabase: GameDatabase?,
    ): Boolean {
        val state = characterState ?: return true
        if (state.inLegacyOfLoathing && !pullableInLoL(itemId, gameDatabase?.item(itemId))) {
            return false
        }
        if (state.inSeaPath && !pullableInSeaPath(itemId)) {
            return false
        }
        if (state.isThrifty) {
            val itemName = gameDatabase?.item(itemId)?.name ?: return false
            if (!ThriftyRequest.isAllowed(RestrictedItemType.ITEMS, itemName)) {
                return false
            }
        }
        return true
    }
}
