package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class ArmoryAndLeggerySyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
        NpcStoreVisitOverlay.resetForTest()
        ShopRowDatabase.resetForTest()
    }

    @Test
    fun visitSyncLearnsUnknownRowAndPulverizedCurrency() {
        registerItems()
        StandardRewardDatabase.loadFromText(
            """
                11504	2024	norm	SC	ROW1454	moss mace
                11528	2025	hard	SC	UNKNOWN	petrified wood war pike
            """.trimIndent(),
            """
                11510	2024	norm	moss mulch
                11534	2025	hard	petrified wood waste parts
            """.trimIndent(),
        )
        CoinmasterDatabase.loadFromText(
            shopsText = "armory\tArmory and Leggery\n",
            coinText = "",
        )
        val prefs = Preferences(MapSettings())
        ArmoryAndLeggerySync.syncFromShopHtml(
            """
                <tr rel="11528">
                <a onClick='javascript:descitem(11528)'><b>petrified wood war pike</b></a>
                <span title="petrified wood waste parts"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=armory&whichrow=1700">
                </tr>
            """.trimIndent(),
            prefs,
            force = true,
        )

        val reward = StandardRewardDatabase.findStandardReward(11528)
        assertNotNull(reward)
        assertEquals("1700", reward.row)
        assertNotNull(StandardRewardDatabase.findStandardPulverized(PETRIFIED_WOOD))
        assertTrue(CoinmasterDatabase.findBuyRowForItem(11528) != null)
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
    }

    @Test
    fun visitSyncLogsToDataLines() {
        registerItems()
        StandardRewardDatabase.loadFromText(
            """
                11528	2025	hard	SC	UNKNOWN	petrified wood war pike
            """.trimIndent(),
            "",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ArmoryAndLeggerySync.syncFromShopHtml(
            """
                <tr rel="11528">
                <a onClick='javascript:descitem(11528)'><b>petrified wood war pike</b></a>
                <span title="petrified wood waste parts"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=armory&whichrow=1700">
                </tr>
            """.trimIndent(),
            prefs,
            force = true,
            sessionLogger = sessionLogger,
        )

        val lines = sessionLogger.recentLines()
        assertTrue(lines.any { it.contains("11534\t2026\thard\tpetrified wood waste parts") })
        assertTrue(lines.any { it.contains("11528\t2025\thard\tSC\t1700\tpetrified wood war pike") })
    }

    @Test
    fun visitSyncRegistersMeatNpcOverlay() {
        registerTestItem(MEAT_ITEM, "brand new meat hat")
        val prefs = Preferences(MapSettings())
        ArmoryAndLeggerySync.syncFromShopHtml(
            """
                <tr rel="$MEAT_ITEM">
                <a onClick='javascript:descitem($MEAT_ITEM)'><b>brand new meat hat</b></a>
                <span title="Meat"><b>500</b></span>
                <form action="shop.php?action=buy&whichshop=armory&whichrow=8888">
                </tr>
            """.trimIndent(),
            prefs,
        )

        assertNotNull(NpcStoreDatabase.itemEntry(MEAT_ITEM))
        assertNotNull(NpcStoreVisitOverlay.toNpcStoreLine(MEAT_ITEM))
    }

    private fun registerItems() {
        registerTestItem(MOSS_MACE, "moss mace")
        registerTestItem(11528, "petrified wood war pike")
        registerTestItem(PETRIFIED_WOOD, "petrified wood waste parts")
    }

    private fun registerTestItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val MOSS_MACE = 11504
        private const val MOSS_MULCH = 11510
        private const val PETRIFIED_WOOD = 11534
        private const val MEAT_ITEM = 99050
    }
}
