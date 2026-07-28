package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class NpcStoreDatabaseContainsTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
    }

    @Test
    fun containsItem_trueWhenItemInStore() {
        ItemDatabase.registerForTest(testItem(9001, "npc shop widget"))
        NpcStoreDatabase.loadFromText(
            """
            Test Shop	testshop	npc shop widget	100
            """.trimIndent(),
        )
        assertTrue(NpcStoreDatabase.containsItem(9001))
    }

    @Test
    fun containsItem_falseWhenUnknownItem() {
        assertFalse(NpcStoreDatabase.containsItem(9999))
    }

    private fun testItem(id: Int, name: String) = ItemData(
        id = id,
        name = name,
        descId = "d$id",
        image = "img",
        primaryUse = ItemPrimaryUse.USABLE,
        secondaryUses = emptySet(),
        access = setOf('t', 'd'),
        autosellPrice = 1,
        plural = null,
    )
}
