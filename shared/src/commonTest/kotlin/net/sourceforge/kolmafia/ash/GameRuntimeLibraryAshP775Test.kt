package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SitCourseChoiceSync

class GameRuntimeLibraryAshP775Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun psychogeologist_course() {
        val prefs = Preferences(MapSettings())
        val removed = mutableListOf<Int>()
        val learned = mutableListOf<Int>()
        assertTrue(
            SitCourseChoiceSync.apply(
                choiceId = 1494,
                decision = 1,
                preferences = prefs,
                removeSkill = { removed += it },
                learnSkill = { learned += it },
            ),
        )
        assertEquals("Psychogeologist", prefs.getString("currentSITSkill", ""))
        assertTrue(prefs.getBoolean("_sitCourseCompleted", false))
        assertEquals(
            listOf(SitCourseChoiceSync.INSECTOLOGIST_ID, SitCourseChoiceSync.CRYPTOBOTANIST_ID),
            removed,
        )
        assertEquals(listOf(SitCourseChoiceSync.PSYCHOGEOLOGIST_ID), learned)
        assertEquals(0, prefs.getInt("skillLevel${SitCourseChoiceSync.INSECTOLOGIST_ID}", -1))
        assertEquals(1, prefs.getInt("skillLevel${SitCourseChoiceSync.PSYCHOGEOLOGIST_ID}", 0))
    }

    @Test
    fun questChoiceRules_wires1494() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1494,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals("Insectologist", prefs.getString("currentSITSkill", ""))
    }
}
