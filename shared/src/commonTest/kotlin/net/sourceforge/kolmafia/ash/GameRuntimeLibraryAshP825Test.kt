package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FantasyRealmChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP825Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mountains_unlocksMineAndConsumesKey() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 4)
        prefs.setString("_frAreasUnlocked", "The Towering Mountains,")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FantasyRealmChoiceSync.apply(
                choiceId = 1282,
                decision = 1,
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(3, prefs.getInt("_frHoursLeft", 0))
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Old Rubee Mine,"))
        assertFalse(prefs.getString("_frAreasUnlocked", "").contains("The Towering Mountains,"))
        assertEquals(listOf(FantasyRealmChoiceSync.FR_KEY to 1), consumed)
    }

    @Test
    fun wood_leaveExitSkipsHourBurn() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 3)
        prefs.setString("_frAreasUnlocked", "The Mystic Wood,")
        assertTrue(FantasyRealmChoiceSync.apply(1283, 11, prefs))
        assertEquals(3, prefs.getInt("_frHoursLeft", 0))
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Mystic Wood,"))
    }

    @Test
    fun swamp_unlocksDragonMoor() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Putrid Swamp,")
        assertTrue(FantasyRealmChoiceSync.apply(1284, 3, prefs))
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Dragon's Moor,"))
    }

    @Test
    fun village_buttonPress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 2)
        prefs.setString("_frAreasUnlocked", "The Cursed Village,")
        assertTrue(FantasyRealmChoiceSync.apply(1285, 10, prefs))
        assertEquals(1, prefs.getInt("_frButtonsPressed", 0))
        assertEquals(1, prefs.getInt("_frHoursLeft", 0))
    }

    @Test
    fun cemetery_unlocksCrypt() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 1)
        prefs.setString("_frAreasUnlocked", "The Sprawling Cemetery,")
        assertTrue(FantasyRealmChoiceSync.apply(1286, 1, prefs))
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Labyrinthine Crypt,"))
    }

    @Test
    fun questChoiceRules_wires1282() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_frHoursLeft", 5)
        prefs.setString("_frAreasUnlocked", "The Towering Mountains,")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1282,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 2,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getString("_frAreasUnlocked", "").contains("The Foreboding Cave,"))
    }
}
