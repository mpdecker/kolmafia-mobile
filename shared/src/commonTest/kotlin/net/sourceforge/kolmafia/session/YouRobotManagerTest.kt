package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YouRobotManagerTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        YouRobotManager.reset()
        prefs = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        YouRobotManager.reset()
    }

    @Test
    fun installPartAndCpu_gatesAndPrefs() {
        YouRobotManager.installUpgradeForTest(YouRobotManager.RobotUpgrade.MANNEQUIN_HEAD, prefs)
        YouRobotManager.installUpgradeForTest(YouRobotManager.RobotUpgrade.BIRD_CAGE, prefs)
        assertTrue(YouRobotManager.canUseFamiliars())
        assertEquals(2, prefs.getInt("youRobotTop", 0))
        assertTrue(YouRobotManager.hasEquipped("Bird Cage"))

        YouRobotManager.installUpgradeForTest(YouRobotManager.RobotUpgrade.BIOMASS_PROCESSING_FUNCTION, prefs)
        assertTrue(YouRobotManager.canUsePotions())
        assertTrue(prefs.getString("youRobotCPUUpgrades", "").contains("robot_potions"))
    }

    @Test
    fun parseAvatar_and_statbot() {
        val html = """
            <img src="otherimages/robot/top2.png">
            <img src="otherimages/robot/left4.png">
            <img src="otherimages/robot/body1.png">
            Current upgrade cost: <b>12 energy</b>
        """.trimIndent()
        assertTrue(YouRobotManager.parseAvatar(html, prefs))
        assertEquals(2, prefs.getInt("youRobotTop", 0))
        assertEquals(4, prefs.getInt("youRobotLeft", 0))
        assertEquals(1, prefs.getInt("youRobotBody", 0))
        assertTrue(YouRobotManager.canUseFamiliars())
        assertTrue(YouRobotManager.parseStatbotCost(html, prefs))
        assertEquals(2, prefs.getInt("statbotUses", 0))
    }

    @Test
    fun restoreFromPreferences() {
        prefs.setInt("youRobotRight", 4)
        prefs.setString("youRobotCPUUpgrades", "robot_shirt,robot_potions")
        YouRobotManager.restoreFromPreferences(prefs)
        assertTrue(YouRobotManager.hasEquipped(YouRobotManager.RobotUpgrade.OMNI_CLAW))
        assertTrue(YouRobotManager.canUsePotions())
        assertTrue(YouRobotManager.hasEquipped(YouRobotManager.RobotUpgrade.TOPOLOGY_GRID))
    }

    @Test
    fun canEquip_requiresMatchingParts() {
        assertFalse(YouRobotManager.canEquip(net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT))
        YouRobotManager.installUpgradeForTest(YouRobotManager.RobotUpgrade.MANNEQUIN_HEAD, prefs)
        assertTrue(YouRobotManager.canEquip(net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT))
    }
}
