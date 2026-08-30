package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [CharPaneRequest.checkInteraction] / [setInteraction] (Phases 2451–2465).
 * Scripts use this for mall / pull / storage gates via [canInteract].
 */
object CharpaneInteraction {

    const val INITIAL_RONIN = 1000
    const val INITIAL_RONIN_GOOCORE = 10000

    fun checkInteraction(state: CharacterState): Boolean {
        if (state.kingLiberated) return true
        if (state.isHardcore) return false
        if (ZodiacSign.find(state.zodiacSign)?.isBadMoon == true) return false
        if (!state.isInRonin) return true
        val path = state.challengePath
        val threshold = if (path.contains("Goo", ignoreCase = true) ||
            path.contains("goocore", ignoreCase = true)
        ) {
            INITIAL_RONIN_GOOCORE
        } else {
            INITIAL_RONIN
        }
        return state.currentRun >= threshold && !state.inPokefam
    }

    fun applyInteraction(
        character: KoLCharacter,
        preferences: Preferences? = null,
    ) {
        val state = character.state.value
        val interact = checkInteraction(state)
        character.setCanInteract(interact)
        preferences?.setBoolean("canInteract", interact)
        if (interact) {
            preferences?.setInt("pullsRemaining", -1)
        }
    }
}
