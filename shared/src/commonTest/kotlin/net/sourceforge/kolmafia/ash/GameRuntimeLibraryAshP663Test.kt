package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.NewYouCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP663Test {

    @Test
    fun revision_phase665() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun sharpenSaw_recordsProgress() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.NEW_YOU, QuestDatabase.STARTED)
        assertTrue(
            NewYouCombatSync.apply(
                html = "You're really sharpening the old saw.  Looks like you've done 1 out of 14!",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("1", prefs.getString("_newYouQuestSharpensDone"))
        assertEquals("14", prefs.getString("_newYouQuestSharpensToDo"))
    }

    @Test
    fun sawComplete_resetsQuest() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_newYouQuestMonster", "skleleton")
        prefs.setString("_newYouQuestSkill", "Tongue of the Walrus")
        prefs.setInt("_newYouQuestSharpensDone", 14)
        prefs.setInt("_newYouQuestSharpensToDo", 14)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.NEW_YOU, "step1")
        assertTrue(
            NewYouCombatSync.apply(
                html = "You did it!  Your saw is so sharp!",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("", prefs.getString("_newYouQuestMonster"))
        assertEquals("", prefs.getString("_newYouQuestSkill"))
        assertEquals(0, prefs.getInt("_newYouQuestSharpensDone"))
        assertEquals(0, prefs.getInt("_newYouQuestSharpensToDo"))
        assertTrue(prefs.getBoolean("_newYouQuestCompleted"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.NEW_YOU))
    }

    @Test
    fun applyCombat_wiresSharpenWithoutWin() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "skleleton",
                won = false,
                preferences = prefs,
                responseText = "You're really sharpening the old saw.  Looks like you've done 3 out of 14!",
            ).advanced,
        )
        assertEquals("3", prefs.getString("_newYouQuestSharpensDone"))
        assertEquals("14", prefs.getString("_newYouQuestSharpensToDo"))
    }

    @Test
    fun withoutSawText_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            NewYouCombatSync.apply(
                html = "You win the fight!",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertFalse(prefs.getBoolean("_newYouQuestCompleted", false))
    }
}
