package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DinseyCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP647Test {

    @Test
    fun revision_phase641() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun finishMessage_setsFifteenAndStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "445",
                "probably not unacceptably racist anymore",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("dinseySocialJusticeIIProgress"))
        assertEquals("step1", db.getProgress(Quest.SOCIAL_JUSTICE_II))
    }

    @Test
    fun startedQuest_incrementsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseySocialJusticeIIProgress", 6)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.STARTED)
        assertTrue(DinseyCombatSync.apply("445", "You win the fight", db, prefs))
        assertEquals(7, prefs.getInt("dinseySocialJusticeIIProgress"))
    }

    @Test
    fun unstartedWithoutMessage_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(DinseyCombatSync.apply("445", "You win the fight", db, prefs))
        assertEquals(0, prefs.getInt("dinseySocialJusticeIIProgress", 0))
    }

    @Test
    fun applyCombatWin_wiresSluice() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SOCIAL_JUSTICE_II, "step1")
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "445",
                responseText = "You win the fight",
            ).advanced,
        )
        assertEquals(1, prefs.getInt("dinseySocialJusticeIIProgress"))
    }
}
