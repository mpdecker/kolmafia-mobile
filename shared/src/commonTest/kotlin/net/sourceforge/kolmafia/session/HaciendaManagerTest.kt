package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class HaciendaManagerTest {

    @Test
    fun parseRoom_marksFightAndDeducesReward() {
        val prefs = Preferences(MapSettings())
        prefs.setString(HaciendaManager.LAYOUT_PREF, "000000000000000000")
        val db = QuestDatabase(prefs)
        HaciendaManager.parseRoom(
            lastChoice = 413,
            lastDecision = 1,
            text = "You encounter a mariachi. Fight!",
            preferences = prefs,
            questDatabase = db,
        )
        assertEquals('F', prefs.getString(HaciendaManager.LAYOUT_PREF, "?")[0])
    }

    @Test
    fun parseRoom_marksClueAndLocatedKey() {
        val prefs = Preferences(MapSettings())
        prefs.setString(HaciendaManager.LAYOUT_PREF, "000000000000000000")
        val logs = mutableListOf<String>()
        HaciendaManager.parseRoom(
            lastChoice = 414,
            lastDecision = 3,
            text = "You have found a clue: a potato peeler",
            preferences = prefs,
            questDatabase = QuestDatabase(prefs),
            sessionLog = logs::add,
        )
        val layout = prefs.getString(HaciendaManager.LAYOUT_PREF, "")
        assertEquals('C', layout[5])
        assertEquals('k', layout[0])
        assertTrue(logs.any { it.contains("potato peeler") })
    }

    @Test
    fun questCompleted_convertsUnknownAndFights() {
        val prefs = Preferences(MapSettings())
        prefs.setString(HaciendaManager.LAYOUT_PREF, "FuF000000000000000")
        HaciendaManager.questCompleted(prefs)
        val layout = prefs.getString(HaciendaManager.LAYOUT_PREF, "")
        assertEquals('r', layout[0])
        assertEquals('C', layout[1])
    }

    @Test
    fun getSpoiler_reportsFightForMarkedSquare() {
        val prefs = Preferences(MapSettings())
        prefs.setString(HaciendaManager.LAYOUT_PREF, "F00000000000000000")
        assertEquals(
            "fight mariachi",
            HaciendaManager.getSpoiler(0, prefs, QuestDatabase(prefs)),
        )
    }

    @Test
    fun preRecording_syncsCastPrefs() {
        val prefs = Preferences(MapSettings())
        val html = """<option value="530">The Ballad of Richie Thingfinder (2/10)</option>"""
        HaciendaManager.preRecording(html, prefs)
        assertEquals(2, prefs.getInt("_thingfinderCasts", -1))
    }
}
