package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ConcoctionDatabase SMITH/SSMITH method gates. */
object SmithingGates {

    const val TENDERIZING_HAMMER = 338

    fun isSmithPermitted(
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        limitMode: String = "none",
    ): Boolean {
        if (accessibleCount(TENDERIZING_HAMMER) > 0) return true
        if (KitchenAutoBuy.willBuyTool(state, prefs, limitMode)) return true
        if (KnollAvailability.isAvailable(state, limitMode) && !state.inZombiecore) return true
        return false
    }

    fun isSSmithPermitted(
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        limitMode: String = "none",
    ): Boolean = isSmithPermitted(state, prefs, accessibleCount, limitMode)
}
