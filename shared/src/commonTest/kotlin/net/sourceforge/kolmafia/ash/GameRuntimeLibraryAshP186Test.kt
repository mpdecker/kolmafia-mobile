package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.PulverizeFlags
import net.sourceforge.kolmafia.data.StandardRewardDatabase

class GameRuntimeLibraryAshP186Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
    }

    @Test
    fun revision_phase195() {
        assertEquals("phase230", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pulverizeAsh_returnsEpicWadAtOneMillion() {
        registerPulverizeFixtures()
        val lib = GameRuntimeLibrary()
        val output = outputLib(
            lib,
            """
            int[item] loot = pulverize(to_item("Chester's sunglasses"));
            print(loot[to_item("epic wad")]);
            """.trimIndent(),
        ).trim()
        assertEquals("1000000", output)
    }

    @Test
    fun pulverizeAsh_nosmashReturnsEmptyAggregate() {
        registerPulverizeFixtures()
        val lib = GameRuntimeLibrary()
        val output = outputLib(
            lib,
            """
            int[item] loot = pulverize(to_item("Mr. Accessory"));
            print(count(loot));
            """.trimIndent(),
        ).trim()
        assertEquals("0", output)
    }

    @Test
    fun pulverizeAsh_clusterEquipmentReturnsHotCluster() {
        registerClusterFixtures()
        val lib = GameRuntimeLibrary()
        val output = outputLib(
            lib,
            """
            int[item] loot = pulverize(to_item("frying brainpan"));
            print(loot[to_item("hot cluster")]);
            """.trimIndent(),
        ).trim()
        assertEquals("1000000", output)
    }

    @Test
    fun pulverizeAsh_bejeweledCufflinksReturnsSplitNuggets() {
        registerBejeweledFixtures()
        val lib = GameRuntimeLibrary()
        val output = outputLib(
            lib,
            """
            int[item] loot = pulverize(to_item("bejeweled cufflinks"));
            print(loot[to_item("hot nuggets")]);
            """.trimIndent(),
        ).trim()
        assertEquals("500000", output)
    }

    @Test
    fun pulverizeAsh_upgradePowderReturnsEmptyAggregate() {
        registerUpgradeFixtures()
        val lib = GameRuntimeLibrary()
        val output = outputLib(
            lib,
            """
            int[item] loot = pulverize(to_item("hot powder"));
            print(count(loot));
            """.trimIndent(),
        ).trim()
        assertEquals("0", output)
    }

    @Test
    fun standardRewardDerive_overlaysAfterPulverizeLoad() {
        registerPulverizeFixtures()
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)
        StandardRewardDatabase.derivePulverization()
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
    }

    private fun registerPulverizeFixtures() {
        registerItem(CHESTERS_SUNGLASSES, "Chester's sunglasses", ItemPrimaryUse.ACCESSORY)
        registerItem(EPIC_WAD, "epic wad", ItemPrimaryUse.SPLEEN)
        registerItem(MR_ACCESSORY, "Mr. Accessory", ItemPrimaryUse.ACCESSORY)
        EquipmentDatabase.loadPulverizeFromText(
            """
                Chester's sunglasses	epic wad
                Mr. Accessory	nosmash
            """.trimIndent(),
        )
    }

    private fun registerClusterFixtures() {
        registerItem(FRYING_BRAINPAN, "frying brainpan", ItemPrimaryUse.WEAPON)
        registerItem(HOT_CLUSTER, "hot cluster", ItemPrimaryUse.USABLE)
        EquipmentDatabase.loadPulverizeFromText(
            """
                frying brainpan	hot cluster
            """.trimIndent(),
        )
    }

    private fun registerBejeweledFixtures() {
        registerItem(BEJEWELED_CUFFLINKS, "bejeweled cufflinks", ItemPrimaryUse.ACCESSORY)
        registerItem(HOT_NUGGETS, "hot nuggets", ItemPrimaryUse.POTION)
        EquipmentDatabase.loadPulverizeFromText(
            """
                bejeweled cufflinks	258112
            """.trimIndent(),
        )
    }

    private fun registerUpgradeFixtures() {
        registerItem(HOT_POWDER, "hot powder", ItemPrimaryUse.POTION)
        EquipmentDatabase.loadPulverizeFromText(
            """
                hot powder	upgrade
            """.trimIndent(),
        )
    }

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val CHESTERS_SUNGLASSES = 3383
        private const val EPIC_WAD = 3316
        private const val MR_ACCESSORY = 194
        private const val MOSS_MACE = 11504
        private const val MOSS_MULCH = 11510
        private const val FRYING_BRAINPAN = 6538
        private const val HOT_CLUSTER = 6551
        private const val BEJEWELED_CUFFLINKS = 3958
        private const val HOT_NUGGETS = 1445
        private const val HOT_POWDER = 1439

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11510	2024	norm	moss mulch
        """.trimIndent()
    }
}
