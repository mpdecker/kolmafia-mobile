package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop KoLCharacter.desertBeachAccessible(). */
object DesertBeachAccessibility {

    fun isAvailable(
        state: CharacterState,
        prefs: Preferences?,
        limitMode: String = state.limitMode,
    ): Boolean {
        if (LimitModeGates.limitZone("Beach", limitMode)) return false
        val lastUnlock = prefs?.getInt("lastDesertUnlock", -1) ?: -1
        return lastUnlock == state.ascensionNumber
    }
}
