package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerMcdAvailabilityTest {

    @Test
    fun mcdAvailable_canadiaSign() {
        val state = CharacterState(zodiacSign = "Blender")
        assertTrue(MaximizerMcdAvailability.mcdAvailable(state, null))
        assertTrue(MaximizerMcdAvailability.maxLevel(state) == 11)
    }

    @Test
    fun mcdAvailable_knollSign() {
        val state = CharacterState(zodiacSign = "Mongoose")
        assertTrue(MaximizerMcdAvailability.mcdAvailable(state, null))
        assertTrue(MaximizerMcdAvailability.maxLevel(state) == 10)
    }

    @Test
    fun mcdAvailable_gnomadsWithDesertUnlock() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("lastDesertUnlock", 1)
        }
        val state = CharacterState(zodiacSign = "Wombat", ascensionNumber = 1)
        assertTrue(MaximizerMcdAvailability.mcdAvailable(state, prefs))
    }

    @Test
    fun mcdUnavailable_badMoonProxy() {
        val state = CharacterState(zodiacSign = "Platypus")
        assertFalse(MaximizerMcdAvailability.mcdAvailable(state, null))
    }
}
