package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.StandardRewardDatabase

class GameRuntimeLibraryAshP184Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
    }

    @Test
    fun revision_phase195() {
        assertEquals("phase200", GameRuntimeLibrary.REVISION)
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
    fun standardRewardDerive_overlaysAfterPulverizeLoad() {
        registerPulverizeFixtures()
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)
        StandardRewardDatabase.derivePulverization()
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
    }

    @Test
    fun getPulverization_matchesLoaderFixtures() {
        registerPulverizeFixtures()
        assertEquals(EPIC_WAD, EquipmentDatabase.getPulverization(CHESTERS_SUNGLASSES))
        assertEquals(USELESS_POWDER, EquipmentDatabase.getPulverization(ANTIQUE_GREAVES))
        assertEquals(-1, EquipmentDatabase.getPulverization(MR_ACCESSORY))
    }

    private fun registerPulverizeFixtures() {
        registerItem(CHESTERS_SUNGLASSES, "Chester's sunglasses", ItemPrimaryUse.ACCESSORY)
        registerItem(EPIC_WAD, "epic wad", ItemPrimaryUse.SPLEEN)
        registerItem(ANTIQUE_GREAVES, "antique greaves", ItemPrimaryUse.PANTS)
        registerItem(USELESS_POWDER, "useless powder", ItemPrimaryUse.USABLE)
        registerItem(MR_ACCESSORY, "Mr. Accessory", ItemPrimaryUse.ACCESSORY)
        registerItem(MOSS_MACE, "moss mace", ItemPrimaryUse.WEAPON)
        registerItem(MOSS_MULCH, "moss mulch", ItemPrimaryUse.USABLE)
        EquipmentDatabase.loadPulverizeFromText(
            """
                Chester's sunglasses	epic wad
                antique greaves	useless powder
                Mr. Accessory	nosmash
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
        private const val ANTIQUE_GREAVES = 1929
        private const val USELESS_POWDER = 1437
        private const val MR_ACCESSORY = 194
        private const val MOSS_MACE = 11504
        private const val MOSS_MULCH = 11510

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11510	2024	norm	moss mulch
        """.trimIndent()
    }
}
