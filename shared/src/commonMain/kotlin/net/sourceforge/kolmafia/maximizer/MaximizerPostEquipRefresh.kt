package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager

/** Desktop `ApiRequest.updateStatus()` parity between equip and non-equipment rebuild (Phase 406/408). */
object MaximizerPostEquipRefresh {

    suspend fun refresh(
        inventoryManager: InventoryManager,
        effectManager: EffectManager?,
    ) {
        inventoryManager.refreshCharacterStatus(effectManager)
    }
}
