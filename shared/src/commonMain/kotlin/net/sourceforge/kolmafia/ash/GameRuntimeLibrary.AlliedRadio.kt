package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.character.CharacterState

internal fun GameRuntimeLibrary.cliAlliedRadio(parameters: String, print: (String) -> Unit) {
    val mgr = alliedRadioManager ?: run {
        print("Allied Radio is not available.")
        return
    }
    val invState = inventoryManager?.state?.value ?: InventoryState()
    val charState = character?.state?.value
    runBlocking {
        mgr.run(parameters, invState, charState, print)
    }
}
