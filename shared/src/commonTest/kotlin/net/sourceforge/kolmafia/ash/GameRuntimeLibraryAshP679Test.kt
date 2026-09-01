package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.WalfordChoiceSync

class GameRuntimeLibraryAshP679Test {

    @Test
    fun revision_phase683() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun collector_startsIceQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            WalfordChoiceSync.apply(
                WalfordChoiceSync.COLLECTOR,
                3,
                "fill that with ice",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.BUCKET))
        assertEquals("ice", prefs.getString("walfordBucketItem"))
        assertTrue(prefs.getBoolean("_walfordQuestStartedToday"))
        assertEquals(0, prefs.getInt("walfordBucketProgress"))
    }

    @Test
    fun collector_decision1Clears() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BUCKET, QuestDatabase.STARTED)
        prefs.setString("walfordBucketItem", "ice")
        prefs.setInt("walfordBucketProgress", 40)
        WalfordChoiceSync.apply(WalfordChoiceSync.COLLECTOR, 1, "", db, prefs)
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.BUCKET))
        assertEquals("", prefs.getString("walfordBucketItem"))
        assertEquals(0, prefs.getInt("walfordBucketProgress"))
    }

    @Test
    fun vykea_raidAndFill() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setInt("walfordBucketProgress", 90)
        WalfordChoiceSync.apply(WalfordChoiceSync.VYKEA, 1, "", db, prefs)
        assertTrue(prefs.getBoolean("_VYKEACafeteriaRaided"))
        assertTrue(
            WalfordChoiceSync.apply(
                WalfordChoiceSync.VYKEA,
                3,
                "(Walford's bucket filled by an additional 20%)",
                db,
                prefs,
            ),
        )
        assertEquals(110, prefs.getInt("walfordBucketProgress"))
        assertEquals("step2", db.getProgress(Quest.BUCKET))
    }

    @Test
    fun iceHotel_roomsRaided() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(WalfordChoiceSync.apply(WalfordChoiceSync.ICE_HOTEL, 5, "", db, prefs))
        assertTrue(prefs.getBoolean("_iceHotelRoomsRaided"))
    }

    @Test
    fun questChoiceRules_wires1114() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = WalfordChoiceSync.COLLECTOR,
                responseText = "Bucket of balls",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals("balls", prefs.getString("walfordBucketItem"))
    }
}
