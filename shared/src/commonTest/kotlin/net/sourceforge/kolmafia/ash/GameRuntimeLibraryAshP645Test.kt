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

class GameRuntimeLibraryAshP645Test {

    @Test
    fun revision_phase641() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun socialJusticeStarted_incrementsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseySocialJusticeIProgress", 4)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.STARTED)
        assertTrue(DinseyCombatSync.apply("443", "You win the fight", db, prefs))
        assertEquals(5, prefs.getInt("dinseySocialJusticeIProgress"))
    }

    @Test
    fun socialJusticeUnstarted_finishMessageSetsStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "443",
                "probably not embarrassingly sexist anymore",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("dinseySocialJusticeIProgress"))
        assertEquals("step1", db.getProgress(Quest.SOCIAL_JUSTICE_I))
    }

    @Test
    fun bargesClear_setsFilthZeroAndFishTrashStep2() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyFilthLevel", 20)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "443",
                "at least the barges aren't getting hung up on it anymore",
                db,
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt("dinseyFilthLevel"))
        assertEquals("step2", db.getProgress(Quest.FISH_TRASH))
    }

    @Test
    fun garbageChunks_decrementsFilthAndSetsStep1() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyFilthLevel", 8)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "443",
                "larger chunks of garbage out of the waterway",
                db,
                prefs,
            ),
        )
        assertEquals(3, prefs.getInt("dinseyFilthLevel"))
        assertEquals("step1", db.getProgress(Quest.FISH_TRASH))
    }

    @Test
    fun garbageChunks_filthDoesNotGoBelowZero() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyFilthLevel", 2)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "443",
                "larger chunks of garbage out of the waterway",
                db,
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt("dinseyFilthLevel"))
    }

    @Test
    fun applyCombatWin_wiresBarges() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SOCIAL_JUSTICE_I, "step1")
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "443",
                responseText = "You win the fight",
            ).advanced,
        )
        assertEquals(1, prefs.getInt("dinseySocialJusticeIProgress"))
    }
}
