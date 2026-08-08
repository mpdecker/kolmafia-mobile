package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.ChezSnooteeDatabase
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.CrimboCafeDatabase
import net.sourceforge.kolmafia.data.HellKitchenDatabase
import net.sourceforge.kolmafia.data.MicroBreweryDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop cafe purchase dispatch for craft-queue drain (Hell's Kitchen / Chez Snootée / Microbrewery / Crimbo Cafe). */
class CafePurchaseRequest(
    private val hellKitchenRequest: HellKitchenRequest,
    private val chezSnooteeRequest: ChezSnooteeRequest,
    private val microBreweryRequest: MicroBreweryRequest,
    private val crimboCafeRequest: CrimboCafeRequest,
) {

    suspend fun purchase(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        inventoryCountById: (Int) -> Int = { 0 },
    ): Result<Unit> {
        val larpCount = CafeAccessibility.larpCount(inventoryCountById)
        return when {
            HellKitchenDatabase.isOnMenu(name) ->
                hellKitchenRequest.purchase(name, type, state, prefs, larpCount)
            ChezSnooteeDatabase.isOnMenu(name) ->
                chezSnooteeRequest.purchase(name, type, state, prefs, larpCount)
            MicroBreweryDatabase.isOnMenu(name) ->
                microBreweryRequest.purchase(name, type, state, prefs, larpCount)
            CrimboCafeDatabase.isOnMenu(name) ->
                crimboCafeRequest.purchase(name, type, state, prefs, larpCount)
            else -> Result.failure(CafeNotOnMenuException(name))
        }
    }
}

class CafeNotOnMenuException(name: String) : Exception("Not on cafe menu: $name")
