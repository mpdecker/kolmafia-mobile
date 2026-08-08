package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.CafeMenuEntry
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.CrimboCafeDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ConcoctionQueueRunner

class CrimboCafeRequest(
    private val cafeRequest: CafeRequest,
) {
    fun onMenu(name: String): Boolean = CrimboCafeDatabase.isOnMenu(name)

    suspend fun purchase(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        larpCount: Int = 0,
    ): Result<Unit> {
        if (!CafeAccessibility.isCrimboCafeAvailable(prefs)) {
            return Result.failure(IllegalStateException("Crimbo Cafe not available"))
        }
        val entry = CrimboCafeDatabase.resolve(name)
            ?: return Result.failure(IllegalStateException("Unknown Crimbo Cafe item: $name"))
        return purchaseEntry(entry, type, state, prefs, larpCount)
    }

    internal suspend fun purchaseEntry(
        entry: CafeMenuEntry,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        larpCount: Int,
    ): Result<Unit> {
        preflight(entry, type, state, prefs, larpCount).onFailure { return Result.failure(it) }
        return cafeRequest.consume(entry.cafeId, entry.whichItem).map { }
    }

    companion object {
        internal fun preflight(
            entry: CafeMenuEntry,
            type: ConcoctionConsumptionType,
            state: CharacterState?,
            prefs: Preferences?,
            larpCount: Int,
        ): Result<Unit> {
            ConcoctionQueueRunner.preflightCafeConsume(entry.name, type, state, prefs)
                .onFailure { return Result.failure(it) }
            if (state != null) {
                val price = CafeAccessibility.discountedPrice(entry.price, larpCount)
                if (state.meat < price) {
                    return Result.failure(IllegalStateException("Insufficient meat for: ${entry.name}"))
                }
            }
            return Result.success(Unit)
        }
    }
}
