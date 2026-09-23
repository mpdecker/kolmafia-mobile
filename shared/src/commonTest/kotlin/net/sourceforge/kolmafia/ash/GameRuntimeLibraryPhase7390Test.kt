package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventures
import net.sourceforge.kolmafia.session.DynamicChoiceSpoilers

/**
 * Dynamic ChoiceAdventures spoilers mega wrap (phases 7331–7390).
 * Parent wrap bumps REVISION to phase7390.
 */
class GameRuntimeLibraryPhase7390Test {

    @Test
    fun revision_isPhase7390() {
        assertEquals("phase7630", GameRuntimeLibrary.REVISION)
        assertEquals("7630", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun skipAdventure_andSolverChoiceSpoilersAreNull() {
        assertEquals("skip adventure", ChoiceAdventures.SKIP_ADVENTURE.name)
        assertEquals(null, ChoiceAdventures.choiceSpoilers(535))
        assertEquals(null, ChoiceAdventures.choiceSpoilers(594))
        assertEquals(2, DynamicChoiceSpoilers.dynamicChoiceOptions(7).size)
    }
}
