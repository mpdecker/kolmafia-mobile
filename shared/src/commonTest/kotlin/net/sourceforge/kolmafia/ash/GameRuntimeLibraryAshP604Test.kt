package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Cell37EscapeSync
import net.sourceforge.kolmafia.quest.MelvinShirtSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP604Test {

    @Test
    fun melvin_startsShirtAndConsumesLetter() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            MelvinShirtSync.applyFromVisit(
                url = "place.php?whichplace=mountains&action=mts_melvin",
                html = "I saw this awesome T-shirt",
                questDatabase = db,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.SHIRT))
        assertEquals(listOf(MelvinShirtSync.LETTER_FOR_MELVIGN to 1), consumed)
    }

    @Test
    fun melvin_finishesWithGarment() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            MelvinShirtSync.applyFromVisit(
                url = "place.php?whichplace=mountains&action=mts_melvin",
                html = "I dogn't have a torso.",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.SHIRT))
    }

    @Test
    fun cell37_filePass_setsStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            Cell37EscapeSync.applyFromVisit(
                url = "cobbsknob.php?action=cell37",
                html = "pass the folder through the little barred window",
                questDatabase = db,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.ESCAPE))
    }

    @Test
    fun cell37_blubber_finishes() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            Cell37EscapeSync.applyFromVisit(
                url = "cobbsknob.php?action=cell37",
                html = "hand Subject 37 the glob of abominable blubber",
                questDatabase = db,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.ESCAPE))
    }
}
