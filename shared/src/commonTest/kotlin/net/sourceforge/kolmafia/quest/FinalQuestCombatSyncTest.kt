package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinalQuestCombatSyncTest {

    private fun prefsAndDb(): Pair<Preferences, QuestDatabase> {
        val prefs = Preferences(MapSettings())
        return prefs to QuestDatabase(prefs)
    }

    @Test
    fun applyCombatWin_lossAtCrowd1_noDecrement() {
        val (prefs, db) = prefsAndDb()
        prefs.setInt("nsContestants1", 3)
        assertFalse(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "ns_01_crowd1",
                monster = "contestant",
                responseText = "",
                won = false,
            ),
        )
        assertEquals(3, prefs.getInt("nsContestants1", 0))
    }

    @Test
    fun applyCombatWin_winAtCrowd1_decrementsCounter() {
        val (prefs, db) = prefsAndDb()
        prefs.setInt("nsContestants1", 3)
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "ns_01_crowd1",
                monster = "contestant",
                responseText = "",
                won = true,
            ),
        )
        assertEquals(2, prefs.getInt("nsContestants1", 0))
    }

    @Test
    fun applyCombatWin_winAtCrowdWithZeroCounter_staysZero() {
        val (prefs, db) = prefsAndDb()
        prefs.setInt("nsContestants1", 0)
        prefs.setInt("nsContestants2", 2)
        prefs.setInt("nsContestants3", 1)
        assertFalse(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "ns_01_crowd1",
                monster = "contestant",
                responseText = "",
                won = true,
            ),
        )
        assertEquals(0, prefs.getInt("nsContestants1", 0))
    }

    @Test
    fun applyCombatWin_winAtCrowd3WhenAllCountersZero_advancesFinalStep2() {
        val (prefs, db) = prefsAndDb()
        prefs.setInt("nsContestants1", 0)
        prefs.setInt("nsContestants2", 0)
        prefs.setInt("nsContestants3", 1)
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "ns_01_crowd3",
                monster = "contestant",
                responseText = "",
                won = true,
            ),
        )
        assertEquals(0, prefs.getInt("nsContestants3", 0))
        assertEquals("step2", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_wallOfSkin_advancesStep7() {
        val (_, db) = prefsAndDb()
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "Wall of Skin",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("step7", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_wallOfMeatWithoutStairsText_noStepChange() {
        val (_, db) = prefsAndDb()
        assertFalse(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "wall of meat",
                responseText = "You beat the wall.",
                won = true,
            ),
        )
        assertEquals("unstarted", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_wallOfMeatWithStairsText_advancesStep8() {
        val (_, db) = prefsAndDb()
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "Wall of Meat",
                responseText = "The stairs to the next floor are clear.",
                won = true,
            ),
        )
        assertEquals("step8", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_wallOfBones_advancesStep9() {
        val (_, db) = prefsAndDb()
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "Wall of Bones",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("step9", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_yourShadow_advancesStep11() {
        val (_, db) = prefsAndDb()
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "Your Shadow",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("step11", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_naughtySorceress3_advancesStep13() {
        val (_, db) = prefsAndDb()
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "Naughty Sorceress (3)",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("step13", db.getProgress(Quest.FINAL))
    }

    @Test
    fun applyCombatWin_topiaryDuck_advancesStep4() {
        val (_, db) = prefsAndDb()
        assertTrue(
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = Preferences(MapSettings()),
                adventureId = "tower",
                monster = "topiary duck",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("step4", db.getProgress(Quest.FINAL))
    }
}
