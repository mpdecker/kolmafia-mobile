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

class GameRuntimeLibraryAshP642Test {

    @Test
    fun revision_phase641() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun junglePun_incrementsWithoutFinishing() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("junglePuns", 3)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply("417", "a jungle pun occurs to you", db, prefs),
        )
        assertEquals(4, prefs.getInt("junglePuns"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.JUNGLE_PUN))
    }

    @Test
    fun junglePun_finishesAtEleven() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("junglePuns", 10)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply("417", "a jungle pun occurs to you", db, prefs),
        )
        assertEquals(11, prefs.getInt("junglePuns"))
        assertEquals("step2", db.getProgress(Quest.JUNGLE_PUN))
    }

    @Test
    fun junglePun_withoutPhraseIsNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(DinseyCombatSync.apply("417", "You win the fight", db, prefs))
        assertEquals(0, prefs.getInt("junglePuns", 0))
    }

    @Test
    fun applyCombatWin_wiresJungle() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("junglePuns", 10)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "417",
                responseText = "a jungle pun occurs to you",
            ).advanced,
        )
        assertEquals("step2", db.getProgress(Quest.JUNGLE_PUN))
    }
}
