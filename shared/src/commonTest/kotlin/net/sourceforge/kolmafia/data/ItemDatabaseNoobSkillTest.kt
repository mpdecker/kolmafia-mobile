package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ItemDatabaseNoobSkillTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun getItemListByNoobSkillId_roundTripsForwardIndex() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 88001,
                name = "noob absorb potion",
                descId = "0",
                image = "potion.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        assertEquals(23001, ItemDatabase.getNoobSkillId(88001))
        assertContentEquals(intArrayOf(88001), ItemDatabase.getItemListByNoobSkillId(23001))
    }

    @Test
    fun getItemListByNoobSkillId_supportsMultipleItemsPerSkill() {
        registerNoobItem(88002, "noob absorb potion b", "1")
        registerNoobItem(88003, "noob absorb potion c", "126")
        assertEquals(23002, ItemDatabase.getNoobSkillId(88002))
        assertEquals(23002, ItemDatabase.getNoobSkillId(88003))
        assertContentEquals(intArrayOf(88002, 88003), ItemDatabase.getItemListByNoobSkillId(23002))
    }

    private fun registerNoobItem(id: Int, name: String, descId: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = descId,
                image = "potion.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
