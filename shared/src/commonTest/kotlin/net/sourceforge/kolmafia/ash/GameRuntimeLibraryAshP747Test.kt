package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LegendaryDigestionChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP747Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun spleenFlip_adjustsOrgans() {
        val prefs = Preferences(MapSettings())
        var fullnessDelta = 0
        var spleenDelta = 0
        assertTrue(
            LegendaryDigestionChoiceSync.apply(
                choiceId = 1599,
                decision = 1,
                preferences = prefs,
                adjustFullness = { fullnessDelta = it },
                adjustSpleen = { spleenDelta = it },
            ),
        )
        assertTrue(prefs.getBoolean("_legendaryNoodlesSpleen"))
        assertEquals(-1, fullnessDelta)
        assertEquals(1, spleenDelta)
    }

    @Test
    fun amygdala_increments() {
        val prefs = Preferences(MapSettings())
        assertTrue(LegendaryDigestionChoiceSync.apply(1599, 2, prefs))
        assertEquals(5, prefs.getInt("legendaryNoodlesAmygdala", 0))
    }

    @Test
    fun skin_and_stomach() {
        val prefs = Preferences(MapSettings())
        assertTrue(LegendaryDigestionChoiceSync.apply(1599, 3, prefs))
        assertEquals(5, prefs.getInt("legendaryNoodlesSkin", 0))
        assertTrue(LegendaryDigestionChoiceSync.apply(1599, 5, prefs))
        assertEquals(3, prefs.getInt("legendaryNoodlesStomach", 0))
    }

    @Test
    fun familiarXp_decisionAccepted() {
        assertTrue(LegendaryDigestionChoiceSync.apply(1599, 4, Preferences(MapSettings())))
    }

    @Test
    fun questChoiceRules_wires1599() {
        val prefs = Preferences(MapSettings())
        var spleen = 0
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1599,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                adjustSpleen = { spleen = it },
            ),
        )
        assertTrue(prefs.getBoolean("_legendaryNoodlesSpleen"))
        assertEquals(1, spleen)
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(LegendaryDigestionChoiceSync.apply(1176, 1, Preferences(MapSettings())))
    }
}
