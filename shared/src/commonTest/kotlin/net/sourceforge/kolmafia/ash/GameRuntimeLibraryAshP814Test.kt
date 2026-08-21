package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ControlPanelChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP814Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesPanelFlagsAndOmega() {
        val prefs = Preferences(MapSettings())
        val html = """
            All-Ranchero FM station: VOLUNTARY
            &pi; sleep-hypnosis generators: ON
            <br>Current power level: 22%</td>
        """.trimIndent()
        assertTrue(
            ControlPanelChoiceSync.applyVisit(
                choiceId = 986,
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("controlPanel1", true))
        assertEquals(true, prefs.getBoolean("controlPanel2", false))
        assertEquals(22, prefs.getInt("controlPanelOmega", 0))
    }

    @Test
    fun visit_omegaDeviceClearsQuests() {
        val prefs = Preferences(MapSettings())
        val quests = QuestDatabase(prefs)
        quests.setProgress(Quest.EVE, QuestDatabase.STARTED)
        assertTrue(
            ControlPanelChoiceSync.applyVisit(
                choiceId = 986,
                html = "Omega device activated",
                preferences = prefs,
                questDatabase = quests,
            ),
        )
        assertEquals(0, prefs.getInt("controlPanelOmega", -1))
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.EVE))
    }

    @Test
    fun post_incrementsOmega() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("controlPanelOmega", 10)
        assertTrue(
            ControlPanelChoiceSync.apply(
                choiceId = 986,
                decision = 1,
                html = "ok",
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_controlPanelUsed", false))
        assertEquals(21, prefs.getInt("controlPanelOmega", 0))
    }

    @Test
    fun questChoiceRules_wires986() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 986,
                responseText = "minimum of 24 hours",
                questDatabase = QuestDatabase(prefs),
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_controlPanelUsed", false))
        assertEquals(0, prefs.getInt("controlPanelOmega", 0))
    }
}
