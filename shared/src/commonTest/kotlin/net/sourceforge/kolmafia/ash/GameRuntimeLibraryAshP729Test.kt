package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SpacegateVaccinatorChoiceSync

class GameRuntimeLibraryAshP729Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesUnlockedVaccines() {
        val prefs = Preferences(MapSettings())
        val html = """
            <select><option value=1 class=button type=submit value="Select Vaccine 1">
            <option value=2 class=button type=submit value="Unlock Vaccine 2">
            <option value=3 class=button type=submit value="Select Vaccine 3">
        """.trimIndent()
        assertTrue(SpacegateVaccinatorChoiceSync.applyVisit(1234, html, prefs))
        assertTrue(prefs.getBoolean("spacegateVaccine1"))
        assertFalse(prefs.getBoolean("spacegateVaccine2"))
        assertTrue(prefs.getBoolean("spacegateVaccine3"))
    }

    @Test
    fun post_unlocksVaccine() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpacegateVaccinatorChoiceSync.apply(
                choiceId = 1234,
                decision = 2,
                html = "New vaccine unlocked!",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("spacegateVaccine2"))
    }

    @Test
    fun post_setsUsedOnEffect() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpacegateVaccinatorChoiceSync.apply(
                choiceId = 1234,
                decision = 1,
                html = "You acquire an effect: Rainbow Vaccine",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_spacegateVaccine"))
    }

    @Test
    fun questChoiceRules_wires1234() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1234,
                responseText = "New vaccine unlocked!",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("spacegateVaccine1"))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            SpacegateVaccinatorChoiceSync.apply(
                choiceId = 1219,
                decision = 1,
                html = "New vaccine unlocked!",
                preferences = prefs,
            ),
        )
    }
}
