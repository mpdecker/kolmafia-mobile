package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarManager

/** Desktop [InventoryManager.getEquippedCount] with Hat Trick + inactive familiar copies. */
object EquippedItemCount {

    fun totalEquippedCount(
        itemId: Int,
        itemName: String,
        equipment: Map<EquipmentSlot, String>,
        characterState: CharacterState?,
        gameDatabase: GameDatabase?,
        familiarManager: FamiliarManager?,
    ): Int {
        var count = OutfitManager.equippedCount(itemName, equipment)
        val state = characterState ?: return count + familiarEquippedCount(itemId, familiarManager, null)
        if (state.inHatTrick) {
            count += state.hatTrickHatIds.count { hatId ->
                gameDatabase?.item(hatId)?.name?.equals(itemName, ignoreCase = true) == true ||
                    hatId == itemId
            }
        }
        count += familiarEquippedCount(itemId, familiarManager, state.familiarId)
        return count
    }

    fun familiarEquippedCount(
        itemId: Int,
        familiarManager: FamiliarManager?,
        activeFamiliarId: Int?,
    ): Int {
        val familiars = familiarManager?.state?.value?.ownedFamiliars.orEmpty()
        return familiars.count { familiar ->
            familiar.id != activeFamiliarId &&
                familiar.equipment?.itemId == itemId
        }
    }
}
