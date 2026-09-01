package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventuresUsed
import net.sourceforge.kolmafia.preferences.Preferences

class BeachManagerParityTest {
    @Test
    fun beachChoiceAccountingUsesFreeWalksAndOnlyChoice1388() {
        assertEquals(0, ChoiceAdventuresUsed.adventuresForChoice(1388, 1, freeBeachWalksUsed = 0))
        assertEquals(1, ChoiceAdventuresUsed.adventuresForChoice(1388, 1, freeBeachWalksUsed = 11))
        assertEquals(0, ChoiceAdventuresUsed.adventuresForChoice(1388, 5, freeBeachWalksUsed = 11))
        assertEquals(0, ChoiceAdventuresUsed.adventuresForChoice(1389, 1))
        assertEquals(0, ChoiceAdventuresUsed.adventuresForChoice(1391, 1))
    }

    @Test
    fun beachEncounterClassificationMatchesDesktop() {
        assertTrue(
            net.sourceforge.kolmafia.request.BeachCombRequest.containsEncounter(
                "choice.php?whichchoice=1388&option=1",
            ),
        )
        assertTrue(
            net.sourceforge.kolmafia.request.BeachCombRequest.containsEncounter(
                "choice.php?whichchoice=1388&option=2",
            ),
        )
        assertFalse(
            net.sourceforge.kolmafia.request.BeachCombRequest.containsEncounter(
                "choice.php?whichchoice=1388&option=4",
            ),
        )
    }

    @Test
    fun canonicalBeachManagerParsesLayoutAndFreeWalks() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            BeachManager.parseCombUsage(
                "(You have 10 free walks down the beach left today.) " +
                    "to the start of the beach to find a nice stretch.",
                prefs,
            ),
        )
        val html = """
            You walk for 420 minutes and find a nice stretch of beach.
            <input name="coords" value="8,4197" title="rough sand with a twinkle"
              src="otherimages/beachcomb/twinkle.gif">
            <input name="coords" value="8,4196" title="rough sand"
              src="otherimages/beachcomb/sand.gif">
        """.trimIndent()
        assertTrue(BeachManager.parseBeachMap(html, prefs))
        assertEquals(1, prefs.getInt("_freeBeachWalksUsed"))
        assertEquals("8:tr", prefs.getString("_beachLayout"))
        assertTrue(prefs.getBoolean("hasTwinkleVision"))
    }
}
