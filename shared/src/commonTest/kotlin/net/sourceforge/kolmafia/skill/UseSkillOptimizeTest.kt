package net.sourceforge.kolmafia.skill

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class UseSkillOptimizeTest {

    @BeforeTest
    fun setUp() {
        UseSkillOptimize.resetForTest()
        UseSkillSync.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        UseSkillOptimize.resetForTest()
        UseSkillSync.resetForTest()
    }

    @Test
    fun findBestTool_fallsBackToWeakestWithoutInventory() {
        val tools = BuffTools.TAMER_TOOLS
        val best = UseSkillOptimize.findBestTool(tools, null, null, null)
        assertEquals(tools.last().itemId, best?.itemId)
    }
}

class UseSkillSyncDeepenTest {

    @BeforeTest
    fun setUp() {
        UseSkillSync.resetForTest()
    }

    @Test
    fun parseResponse_songCapStops() {
        UseSkillSync.noteCast(6003, 1)
        val stop = UseSkillSync.parseResponse(
            "skills.php?action=Skillz&whichskill=6003&quantity=1",
            "You can't fit anymore songs in your head right now.",
        )
        assertTrue(stop)
        assertTrue(UseSkillSync.lastUpdate.contains("songs", ignoreCase = true))
    }

    @Test
    fun parseResponse_missingEquipment() {
        UseSkillSync.noteCast(2000, 1)
        val stop = UseSkillSync.parseResponse(
            "skills.php?whichskill=2000",
            "You need special equipment to cast that buff.",
        )
        assertTrue(stop)
        assertEquals("Missing required equipment", UseSkillSync.lastUpdate)
    }

    @Test
    fun parseResponse_deductsMpAndRegistersCastPref() {
        DailyLimitDatabase.registerCastPrefForTest(4019, "_testCasts")
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        char.updateHpMp(10, 10, 50, 50)
        UseSkillSync.noteCast(4019, 2)
        val stop = UseSkillSync.parseResponse(
            urlString = "skills.php?whichskill=4019&quantity=2",
            responseText = "You cast the skill successfully.",
            preferences = prefs,
            character = char,
            mpCostPerCast = 5,
        )
        assertEquals(false, stop)
        assertEquals(40, char.state.value.currentMp)
        assertEquals(2, prefs.getInt("_testCasts", 0))
        assertEquals(4019, prefs.getInt("lastSkillUsed", -1))
    }

    @Test
    fun castGates_rejectsWhenOverMax() {
        val prefs = Preferences(MapSettings())
        DailyLimitDatabase.registerCastPrefForTest(1111, "_gateCast")
        prefs.setInt("_gateCast", 3)
        prefs.setInt("_gateCast_max", 3)
        assertEquals(false, UseSkillCastGates.maxCastsAllowed(1111, 1, prefs))
        assertEquals(true, UseSkillCastGates.maxCastsAllowed(1111, 1, null))
    }
}
