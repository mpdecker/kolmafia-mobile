package net.sourceforge.kolmafia.character

/** Desktop KoLCharacter.getAbsorbsLimit / absorb budget for Gelatinous Noob (Phase 388). */
object NoobcoreAbsorbs {

    fun absorbsLimit(level: Int): Int = if (level > 12) 15 else level + 2

    fun absorbsRemaining(state: CharacterState): Int =
        kotlin.math.max(0, absorbsLimit(state.level) - state.absorbs)
}
