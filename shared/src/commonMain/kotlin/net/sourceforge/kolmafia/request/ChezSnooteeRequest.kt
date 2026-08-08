package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.CafeMenuEntry
import net.sourceforge.kolmafia.data.ChezSnooteeDatabase
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.preferences.Preferences

open class ChezSnooteeRequest(
    private val hellKitchenRequest: HellKitchenRequest,
) {
    fun onMenu(name: String): Boolean = ChezSnooteeDatabase.isOnMenu(name)

    open suspend fun purchase(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        larpCount: Int = 0,
    ): Result<Unit> {
        if (!CafeAccessibility.isChezSnooteeAvailable(state)) {
            return Result.failure(IllegalStateException("Chez Snootée not available"))
        }
        val entry = ChezSnooteeDatabase.resolve(name)
            ?: return Result.failure(IllegalStateException("Unknown Chez Snootée item: $name"))
        return hellKitchenRequest.purchaseEntry(entry, type, state, prefs, larpCount)
    }
}
