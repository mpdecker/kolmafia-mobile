package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.WalfordBucketCombatSync

class GameRuntimeLibraryAshP640Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun takeBack_setsFullAndStep2() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(WalfordBucketCombatSync.PREF, 40)
        val db = QuestDatabase(prefs)
        assertTrue(
            WalfordBucketCombatSync.apply(
                "455",
                "you should take it back to Walford!",
                db,
                prefs,
            ),
        )
        assertEquals(100, prefs.getInt(WalfordBucketCombatSync.PREF))
        assertEquals("step2", db.getProgress(Quest.BUCKET))
    }

    @Test
    fun additionalFill_incrementsAndFinishesAt100() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(WalfordBucketCombatSync.PREF, 90)
        val db = QuestDatabase(prefs)
        assertTrue(
            WalfordBucketCombatSync.apply(
                "456",
                "(Walford's bucket filled by an additional 15%)",
                db,
                prefs,
            ),
        )
        assertEquals(105, prefs.getInt(WalfordBucketCombatSync.PREF))
        assertEquals("step2", db.getProgress(Quest.BUCKET))
    }

    @Test
    fun firstFill_doesNotFinish() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            WalfordBucketCombatSync.apply(
                "457",
                "(Walford's bucket filled by 20%)",
                db,
                prefs,
            ),
        )
        assertEquals(20, prefs.getInt(WalfordBucketCombatSync.PREF))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.BUCKET))
    }

    @Test
    fun applyCombatWin_wiresIceHotel() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "455",
                responseText = "(Walford's bucket filled by 10%)",
            ).advanced,
        )
        assertEquals(10, prefs.getInt(WalfordBucketCombatSync.PREF))
    }

    @Test
    fun otherLocation_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            WalfordBucketCombatSync.apply(
                "325",
                "(Walford's bucket filled by 10%)",
                db,
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt(WalfordBucketCombatSync.PREF, 0))
    }
}
