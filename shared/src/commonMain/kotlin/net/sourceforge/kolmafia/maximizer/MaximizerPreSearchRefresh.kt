package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager

/** Desktop `ApiRequest.updateStatus()` parity at maximize/speculate entry (Phase 409). */
object MaximizerPreSearchRefresh {

    suspend fun refresh(
        inventoryManager: InventoryManager,
        effectManager: EffectManager?,
    ) {
        inventoryManager.refreshCharacterStatus(effectManager)
    }
}
