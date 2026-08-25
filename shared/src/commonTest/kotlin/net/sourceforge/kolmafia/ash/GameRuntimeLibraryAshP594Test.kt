package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FriarsQuestSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP594Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun elbowNc_stampsTurns() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FriarsQuestSync.applyFromAdventure(
                adventureId = "539",
                html = "Deep Imp Act happens",
                preferences = prefs,
                getTurns = { 12 },
            ),
        )
        assertEquals(12, prefs.getInt("lastFriarsElbowNC", -1))
    }

    @Test
    fun heartNc_stampsTurns() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FriarsQuestSync.applyFromAdventure(
                adventureId = "540",
                html = "Moon Over the Dark Heart",
                preferences = prefs,
                getTurns = { 7 },
            ),
        )
        assertEquals(7, prefs.getInt("lastFriarsHeartNC", -1))
    }

    @Test
    fun ceremony_finishesFriarQuest() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("knownAscensions", 3)
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FriarsQuestSync.applyCeremony(
                url = "friars.php",
                html = "Thank you, Adventurer.",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.FRIAR))
        assertEquals(3, prefs.getInt("lastFriarCeremonyAscension", -1))
        assertEquals(
            listOf(
                FriarsQuestSync.DODECAGRAM to 1,
                FriarsQuestSync.CANDLES to 1,
                FriarsQuestSync.BUTTERKNIFE to 1,
            ),
            consumed,
        )
    }
}
