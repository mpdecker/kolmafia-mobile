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

class GameRuntimeLibraryAshP646Test {

    @Test
    fun revision_phase641() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pretendFun_increments() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("dinseyFunProgress", 2)
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply("444", "You pretend to be having a good time", db, prefs),
        )
        assertEquals(3, prefs.getInt("dinseyFunProgress"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.ZIPPITY_DOO_DAH))
    }

    @Test
    fun excitedCrowd_setsFifteenAndStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DinseyCombatSync.apply(
                "444",
                "The surrounding crowd seems to be pretty excited about the ride",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("dinseyFunProgress"))
        assertEquals("step2", db.getProgress(Quest.ZIPPITY_DOO_DAH))
    }

    @Test
    fun withoutFunHtml_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(DinseyCombatSync.apply("444", "You win the fight", db, prefs))
        assertEquals(0, prefs.getInt("dinseyFunProgress", 0))
    }

    @Test
    fun applyCombatWin_wiresTeacups() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "444",
                responseText = "You pretend to be having a good time",
            ).advanced,
        )
        assertEquals(1, prefs.getInt("dinseyFunProgress"))
    }
}
