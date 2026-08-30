package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.JourneymanDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import kotlinx.coroutines.test.runTest

class JourneyManagerTest {

    @Test
    fun turnThresholds_matchDesktopCadence() {
        assertEquals(4, JourneyManager.TURN_THRESHOLDS[0])
        assertEquals(24, JourneyManager.TURN_THRESHOLDS[5])
    }

    @Test
    fun expectedSkillAtTurn_resolvesLoadedData() = runTest {
        JourneymanDatabase.resetForTest()
        JourneymanDatabase.injectForTest(
            JourneymanDatabase.parseForTest(
                """
                Seal Clubber	Barrrney's Barrr	1	[6032]Accordion Bash
                """.trimIndent(),
                validateReferences = false,
            ),
        )
        val skill = JourneyManager.expectedSkillAtTurn(
            "Barrrney's Barrr",
            CharacterClass.SEAL_CLUBBER,
            4,
        )
        assertEquals("Accordion Bash", skill)
    }

    @Test
    fun recordAdventureTurn_setsPrefOnThreshold() = runTest {
        JourneymanDatabase.resetForTest()
        JourneymanDatabase.injectForTest(
            JourneymanDatabase.parseForTest(
                """
                Seal Clubber	Barrrney's Barrr	2	[6033]Some Skill
                """.trimIndent(),
                validateReferences = false,
            ),
        )
        val prefs = Preferences(MapSettings())
        val logs = mutableListOf<String>()
        JourneyManager.recordAdventureTurn(
            locationName = "Barrrney's Barrr",
            turnsSpent = 8,
            characterClass = CharacterClass.SEAL_CLUBBER,
            preferences = prefs,
            sessionLog = logs::add,
        )
        assertTrue(logs.any { it.contains("Some Skill") })
        assertTrue(prefs.getBoolean(JourneyManager.journeymanTurnPref("Barrrney's Barrr", 1), false))
    }

    @Test
    fun isJourneymanPath_checksAscensionPath() {
        assertTrue(JourneyManager.isJourneymanPath(AscensionPath.JOURNEYMAN))
        assertEquals(false, JourneyManager.isJourneymanPath(AscensionPath.STANDARD))
    }
}
