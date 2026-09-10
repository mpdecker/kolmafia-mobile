package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.MicroBreweryDatabase
import net.sourceforge.kolmafia.preferences.Preferences

open class MicroBreweryRequest(
    private val hellKitchenRequest: HellKitchenRequest,
    private val cafeRequest: CafeRequest? = null,
) {
    fun onMenu(name: String): Boolean = MicroBreweryDatabase.isOnMenu(name)

    /** Desktop [MicroBreweryRequest.getDailySpecial] — visit menu when pref empty. */
    open suspend fun ensureDailySpecial(state: CharacterState?, prefs: Preferences?): String? {
        if (!CafeAccessibility.isMicroBreweryAvailable(state, prefs)) return null
        CafeDailySpecialSync.currentSpecialName(prefs)?.let { return it }
        cafeRequest?.visitMenu("2", prefs)
        return CafeDailySpecialSync.currentSpecialName(prefs)
    }

    open suspend fun purchase(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        larpCount: Int = 0,
    ): Result<Unit> {
        if (!CafeAccessibility.isMicroBreweryAvailable(state, prefs)) {
            return Result.failure(IllegalStateException("Micromicrobrewery not available"))
        }
        val entry = MicroBreweryDatabase.resolve(name)
            ?: return Result.failure(IllegalStateException("Unknown Microbrewery item: $name"))
        return hellKitchenRequest.purchaseEntry(entry, type, state, prefs, larpCount)
    }
}
