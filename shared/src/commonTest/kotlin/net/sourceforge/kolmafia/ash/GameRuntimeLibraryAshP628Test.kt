package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemEquippedSync

class GameRuntimeLibraryAshP628Test {

    @Test
    fun revision_phase629() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun goreBucket_startsGore() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.GORE_BUCKET, db))
        assertEquals("step1", db.getProgress(Quest.GORE))
    }

    @Test
    fun cassette_startsJunglePun() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.MINI_CASSETTE_RECORDER, db))
        assertEquals("step1", db.getProgress(Quest.JUNGLE_PUN))
    }

    @Test
    fun gpsWatch_startsOutOfOrder() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.GPS_WATCH, db))
        assertEquals("step1", db.getProgress(Quest.OUT_OF_ORDER))
    }

    @Test
    fun trashNet_startsFishTrash() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.TRASH_NET, db))
        assertEquals("step1", db.getProgress(Quest.FISH_TRASH))
    }

    @Test
    fun lubeShoes_startsSuperLuber() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.LUBE_SHOES, db))
        assertEquals("step1", db.getProgress(Quest.SUPER_LUBER))
    }

    @Test
    fun mascotMask_startsZippity() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.MASCOT_MASK, db))
        assertEquals("step1", db.getProgress(Quest.ZIPPITY_DOO_DAH))
    }

    @Test
    fun walfordsBucket_startsBucket() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.WALFORDS_BUCKET, db))
        assertEquals("step1", db.getProgress(Quest.BUCKET))
    }

    @Test
    fun unknownItem_isNoOp() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertFalse(QuestItemEquippedSync.apply(1, db))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GORE))
    }

    @Test
    fun setQuestIfBetter_doesNotRegress() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.GORE, "step2")
        assertTrue(QuestItemEquippedSync.apply(QuestItemEquippedSync.GORE_BUCKET, db))
        assertEquals("step2", db.getProgress(Quest.GORE))
    }
}
