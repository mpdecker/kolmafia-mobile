package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.inventory.LimitModeGates

/** Desktop KoLCharacter.knollAvailable(). */
object KnollAvailability {

    private val KNOLL_SIGNS = setOf(
        ZodiacSign.MONGOOSE,
        ZodiacSign.WALLABY,
        ZodiacSign.VOLE,
    )

    fun isAvailable(state: CharacterState, limitMode: String = state.limitMode): Boolean {
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        if (sign !in KNOLL_SIGNS) return false
        if (state.isKingdomOfExploathing) return false
        if (LimitModeGates.limitZone("MusSign", limitMode)) return false
        return true
    }
}
