package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class EquipmentDatabasePulverizeTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun loadPulverizeFromText_mapsEpicWadAndUselessPowder() {
        registerItem(CHESTERS_SUNGLASSES, "Chester's sunglasses")
        registerItem(EPIC_WAD, "epic wad")
        registerItem(ANTIQUE_GREAVES, "antique greaves")
        registerItem(USELESS_POWDER, "useless powder")

        EquipmentDatabase.loadPulverizeFromText(
            """
                Chester's sunglasses	epic wad
                antique greaves	useless powder
            """.trimIndent(),
        )

        assertEquals(EPIC_WAD, EquipmentDatabase.getPulverization(CHESTERS_SUNGLASSES))
        assertEquals(USELESS_POWDER, EquipmentDatabase.getPulverization(ANTIQUE_GREAVES))
    }

    @Test
    fun loadPulverizeFromText_mapsNosmashAndUpgrade() {
        registerItem(MR_ACCESSORY, "Mr. Accessory")
        registerItem(HOT_POWDER, "hot powder")

        EquipmentDatabase.loadPulverizeFromText(
            """
                Mr. Accessory	nosmash
                hot powder	upgrade
            """.trimIndent(),
        )

        assertEquals(-1, EquipmentDatabase.getPulverization(MR_ACCESSORY))
        val upgrade = EquipmentDatabase.getPulverization(HOT_POWDER)
        assertEquals(PulverizeFlags.MALUS_UPGRADE, upgrade and PulverizeFlags.MALUS_UPGRADE)
        assertEquals(PulverizeFlags.ELEM_HOT, upgrade and PulverizeFlags.ELEM_HOT)
    }

    @Test
    fun loadPulverizeFromText_mapsClusterRow() {
        registerItem(FRYING_BRAINPAN, "frying brainpan")

        EquipmentDatabase.loadPulverizeFromText(
            """
                frying brainpan	hot cluster
            """.trimIndent(),
        )

        val pulver = EquipmentDatabase.getPulverization(FRYING_BRAINPAN)
        assertEquals(PulverizeFlags.YIELD_1C, pulver and PulverizeFlags.YIELD_1C)
        assertEquals(PulverizeFlags.ELEM_HOT, pulver and PulverizeFlags.ELEM_HOT)
    }

    @Test
    fun loadPulverizeFromText_mapsBejeweledCufflinksNumericBitmask() {
        registerItem(BEJEWELED_CUFFLINKS, "bejeweled cufflinks")

        EquipmentDatabase.loadPulverizeFromText(
            """
                bejeweled cufflinks	258112
            """.trimIndent(),
        )

        assertEquals(PulverizeFlags.PULVERIZE_BITS or 258112, EquipmentDatabase.getPulverization(BEJEWELED_CUFFLINKS))
    }

    @Test
    fun loadPulverizeFromText_mapsNumericBitmask() {
        registerItem(TEST_ITEM, "test sword")

        EquipmentDatabase.loadPulverizeFromText(
            """
                test sword	42
            """.trimIndent(),
        )

        assertEquals(PulverizeFlags.PULVERIZE_BITS or 42, EquipmentDatabase.getPulverization(TEST_ITEM))
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.WEAPON,
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
        private const val HOT_POWDER = 1439
        private const val FRYING_BRAINPAN = 6538
        private const val BEJEWELED_CUFFLINKS = 3958
        private const val TEST_ITEM = 9001
    }
}
