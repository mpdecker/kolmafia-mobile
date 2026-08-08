package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.CafeMenuEntry
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.HellKitchenDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ConcoctionQueueRunner

open class HellKitchenRequest(
    private val cafeRequest: CafeRequest,
) {
    fun onMenu(name: String): Boolean = HellKitchenDatabase.isOnMenu(name)

    open suspend fun purchase(
        name: String,
        type: ConcoctionConsumptionType,
        state: CharacterState?,
        prefs: Preferences?,
        larpCount: Int = 0,
    ): Result<Unit> {
        if (!CafeAccessibility.isHellKitchenAvailable(state)) {
            return Result.failure(IllegalStateException("Hell's Kitchen not available"))
        }
        val entry = HellKitchenDatabase.resolve(name)
            ?: return Result.failure(IllegalStateException("Unknown Hell's Kitchen item: $name"))
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
