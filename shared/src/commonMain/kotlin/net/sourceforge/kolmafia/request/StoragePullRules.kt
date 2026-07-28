package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.BooleanModifier

/** Desktop [StorageRequest.isFreePull] / [isNoPull] classification. */
object StoragePullRules {

    const val BORIS_HELM_ID = 5648
    const val BORIS_HELM_ASKEW_ID = 5650
    const val JARLS_PAN_ID = 6305
    const val JARLS_PAN_COSMIC_ID = 6304
    const val PETE_JACKET_ID = 7250
    const val PETE_JACKET_POPPED_ID = 7267

    data class StorageContents(
        val storage: Map<Int, Int>,
        val freepulls: Map<Int, Int>,
    )

    fun canInteract(characterState: CharacterState?): Boolean =
        characterState?.let { !it.isHardcore && !it.isInRonin } ?: true

    fun classifyContents(
        raw: Map<Int, Int>,
        characterState: CharacterState?,
    ): StorageContents {
        if (canInteract(characterState)) {
            return StorageContents(storage = raw, freepulls = emptyMap())
        }
        val storage = mutableMapOf<Int, Int>()
        val freepulls = mutableMapOf<Int, Int>()
        for ((itemId, qty) in raw) {
            when {
                isNoPull(itemId) -> Unit
                isFreePull(itemId, characterState) -> freepulls[itemId] = qty
                else -> storage[itemId] = qty
            }
        }
        return StorageContents(storage, freepulls)
    }

    fun isFreePull(itemId: Int, characterState: CharacterState?): Boolean {
        val state = characterState ?: return false
        if ((itemId == BORIS_HELM_ID || itemId == BORIS_HELM_ASKEW_ID) && !state.isAxecore) {
            return false
        }
        if ((itemId == JARLS_PAN_ID || itemId == JARLS_PAN_COSMIC_ID) &&
            state.ascensionPath != AscensionPath.AVATAR_OF_JARLSBERG
        ) {
            return false
        }
        if ((itemId == PETE_JACKET_ID || itemId == PETE_JACKET_POPPED_ID) &&
            state.ascensionPath != AscensionPath.AVATAR_OF_SNEAKY_PETE
        ) {
            return false
        }
        val name = ItemDatabase.getById(itemId)?.name ?: return false
        return ModifierDatabase.hasBooleanModifier(name, BooleanModifier.FREE_PULL)
    }

    fun isNoPull(itemId: Int): Boolean {
        val name = ItemDatabase.getById(itemId)?.name ?: return false
        return ModifierDatabase.hasBooleanModifier(name, BooleanModifier.NOPULL)
    }
}
