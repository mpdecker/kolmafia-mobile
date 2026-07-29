package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.PulverizeFlags

class GameRuntimeLibraryAshP220Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun getRelated_pulverize_decodesBejeweledCufflinks() {
        registerWeapon(BEJEWELED_CUFFLINKS, "bejeweled cufflinks")
        registerItem(HOT_NUGGETS, "hot nuggets", ItemPrimaryUse.SPLEEN)
        EquipmentDatabase.loadPulverizeFromText(
            """
                bejeweled cufflinks	258112
            """.trimIndent(),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("bejeweled cufflinks");
                print(contains_key(get_related(i, "pulverize"), to_item("hot nuggets")));
            """.trimIndent(),
        )
        assertEquals("true", out.trim())
    }

    @Test
    fun getRelated_unknownType_returnsEmptyAggregate() {
        registerWeapon(TEST_ITEM, "test sword")
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("test sword");
                print(count(get_related(i, "fold")));
            """.trimIndent(),
        )
        assertEquals("0", out.trim())
    }

    private fun registerWeapon(id: Int, name: String) {
        registerItem(id, name, ItemPrimaryUse.WEAPON)
        EquipmentDatabase.registerForTest(
            id,
            net.sourceforge.kolmafia.data.EquipmentData(name, 120, null, 1, "sword"),
        )
        EquipmentDatabase.addPulverization(id, PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.YIELD_1P)
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
        private const val BEJEWELED_CUFFLINKS = 3958
        private const val HOT_NUGGETS = 1445
        private const val TEST_ITEM = 9001
    }
}
