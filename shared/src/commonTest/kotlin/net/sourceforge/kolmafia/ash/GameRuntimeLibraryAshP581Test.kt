package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HiddenCityItemSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP581Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mossSphere_setsProgress7AndFinishesCurses() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityItemSync.applyItemAcquire(
                HiddenCityItemSync.MOSS_COVERED_STONE_SPHERE,
                db,
                prefs,
            ),
        )
        assertEquals(7, prefs.getInt("hiddenApartmentProgress", 0))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.CURSES))
    }

    @Test
    fun allFourSpheres_setWorshipStep4() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        HiddenCityItemSync.applyItemAcquire(HiddenCityItemSync.MOSS_COVERED_STONE_SPHERE, db, prefs)
        HiddenCityItemSync.applyItemAcquire(HiddenCityItemSync.DRIPPING_STONE_SPHERE, db, prefs)
        HiddenCityItemSync.applyItemAcquire(HiddenCityItemSync.CRACKLING_STONE_SPHERE, db, prefs)
        HiddenCityItemSync.applyItemAcquire(HiddenCityItemSync.SCORCHED_STONE_SPHERE, db, prefs)
        assertEquals("step4", db.getProgress(Quest.WORSHIP))
    }

    @Test
    fun cracklingSphere_consumesMcCluskyFile() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            HiddenCityItemSync.applyItemAcquire(
                HiddenCityItemSync.CRACKLING_STONE_SPHERE,
                db,
                prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(HiddenCityItemSync.MCCLUSKY_FILE to 1))
        assertEquals(7, prefs.getInt("hiddenOfficeProgress", 0))
    }

    @Test
    fun ancientAmulet_finishesWorshipAndSetsProgress8() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityItemSync.applyItemAcquire(
                HiddenCityItemSync.ANCIENT_AMULET,
                db,
                prefs,
            ),
        )
        assertEquals(8, prefs.getInt("hiddenApartmentProgress", 0))
        assertEquals(8, prefs.getInt("hiddenHospitalProgress", 0))
        assertEquals(8, prefs.getInt("hiddenOfficeProgress", 0))
        assertEquals(8, prefs.getInt("hiddenBowlingAlleyProgress", 0))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.WORSHIP))
    }
}
