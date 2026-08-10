package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.DesertBeachAccessibility

/** Desktop KoLCharacter.mcdAvailable() / MCD dial max for maximizer boosts. */
object MaximizerMcdAvailability {

    fun mcdAvailable(state: CharacterState, prefs: Preferences?): Boolean {
        if (canadiaAvailable(state)) return true
        if (knollAvailable(state) && !state.inGLover) return true
        if (gnomadsAvailable(state) &&
            DesertBeachAccessibility.isAvailable(state, prefs)
        ) {
            return true
        }
        return false
    }

    fun maxLevel(state: CharacterState): Int =
        if (canadiaAvailable(state)) 11 else 10

    private fun canadiaAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT ||
            sign == ZodiacSign.VOLE
    }

    private fun knollAvailable(state: CharacterState): Boolean =
        state.knollAvailable && !state.isKingdomOfExploathing

    private fun gnomadsAvailable(state: CharacterState): Boolean {
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.WOMBAT ||
            sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT
    }
}
