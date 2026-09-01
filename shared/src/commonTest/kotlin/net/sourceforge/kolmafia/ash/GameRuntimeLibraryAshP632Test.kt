package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP632Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun tacoFish_addsCapturedMeat() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("tacoDanFishMeat", 40)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "taco fish",
                "You gain 25 taco fish meat",
                db,
                prefs,
            ),
        )
        assertEquals(65, prefs.getInt("tacoDanFishMeat"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.TACO_DAN_FISH))
    }

    @Test
    fun tacoFish_finishesAtThreeHundred() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("tacoDanFishMeat", 280)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "taco fish",
                "You gain 30 taco fish meat",
                db,
                prefs,
            ),
        )
        assertEquals(310, prefs.getInt("tacoDanFishMeat"))
        assertEquals("step1", db.getProgress(Quest.TACO_DAN_FISH))
    }

    @Test
    fun tacoFish_withoutMeatIsNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(AirportCombatSync.apply("taco fish", "You win the fight", db, prefs))
        assertEquals(0, prefs.getInt("tacoDanFishMeat", 0))
    }
}
