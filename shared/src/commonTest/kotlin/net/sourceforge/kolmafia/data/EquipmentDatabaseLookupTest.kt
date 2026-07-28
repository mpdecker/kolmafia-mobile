package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EquipmentDatabaseLookupTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun parsesHandsAndTypeColumn() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 2001,
                name = "25-meat staff",
                descId = "d1",
                image = "img",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(
            itemId = 2001,
            equipment = EquipmentData(
                name = "25-meat staff",
                power = 70,
                statRequirement = "Mus: 20",
                hands = 2,
                itemType = "staff",
            ),
        )
        assertEquals(70, EquipmentDatabase.getPower(2001))
        assertEquals(2, EquipmentDatabase.getHands(2001))
        assertEquals("staff", EquipmentDatabase.getItemType(2001))
        assertEquals(WeaponStat.MUSCLE, EquipmentDatabase.getWeaponStat(2001))
    }

    @Test
    fun itemTypeFallsBackToPrimaryUse() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 2002,
                name = "tasty snack",
                descId = "d2",
                image = "img",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 5,
                plural = null,
            ),
        )
        assertEquals("food", EquipmentDatabase.getItemType(2002))
        assertEquals(WeaponStat.NONE, EquipmentDatabase.getWeaponStat(2002))
    }

    @Test
    fun moxieWeaponStatFromRequirement() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 2003,
                name = "toy pistol",
                descId = "d3",
                image = "img",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 50,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(
            itemId = 2003,
            equipment = EquipmentData(
                name = "toy pistol",
                power = 10,
                statRequirement = "Mox: 5",
                hands = 1,
                itemType = "pistol",
            ),
        )
        assertEquals(WeaponStat.MOXIE, EquipmentDatabase.getWeaponStat(2003))
    }
}
