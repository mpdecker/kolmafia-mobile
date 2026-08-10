package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquipmentDatabaseTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun nextEquipmentItemId_skipsNonEquipment() {
        registerItem(10, "plain food", ItemPrimaryUse.FOOD)
        registerItem(11, "myst hat", ItemPrimaryUse.HAT)
        EquipmentDatabase.registerForTest(
            11,
            EquipmentData("myst hat", 100, null, 0, "hat"),
        )
        registerItem(12, "fam larva", ItemPrimaryUse.FAMILIAR)
        registerItem(13, "peashooter", ItemPrimaryUse.SIXGUN)

        assertEquals(11, EquipmentDatabase.nextEquipmentItemId(0))
        assertEquals(12, EquipmentDatabase.nextEquipmentItemId(11))
        assertEquals(13, EquipmentDatabase.nextEquipmentItemId(12))
        assertEquals(-1, EquipmentDatabase.nextEquipmentItemId(13))
    }

    @Test
    fun allEquipmentItemIds_returnsRegisteredEquipmentAndSpecialUses() {
        registerItem(21, "acc ring", ItemPrimaryUse.ACCESSORY)
        EquipmentDatabase.registerForTest(
            21,
            EquipmentData("acc ring", 50, null, 0, "accessory"),
        )
        registerItem(22, "sixgun", ItemPrimaryUse.SIXGUN)

        assertEquals(listOf(21, 22), EquipmentDatabase.allEquipmentItemIds().toList())
    }

    @Test
    fun contains_reflectsRegisteredEquipment() {
        registerItem(31, "shirt", ItemPrimaryUse.SHIRT)
        EquipmentDatabase.registerForTest(
            31,
            EquipmentData("shirt", 10, null, 0, "shirt"),
        )
        assertTrue(EquipmentDatabase.contains(31))
        assertFalse(EquipmentDatabase.contains(32))
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
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
