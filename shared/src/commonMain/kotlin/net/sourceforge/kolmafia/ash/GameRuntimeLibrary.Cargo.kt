package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryState

internal fun GameRuntimeLibrary.cliCargo(parameters: String, print: (String) -> Unit) {
    val mgr = cargoCultManager ?: run {
        print("Cargo cult automation is not available.")
        return
    }
    val invState = inventoryManager?.state?.value ?: InventoryState()
    val charState = character?.state?.value
    runBlocking {
        mgr.run(parameters, invState, charState, print)
    }
}
