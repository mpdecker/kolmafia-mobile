package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoinmasterDatabaseTest {

    private val sampleShops = """
        mystic	The Crackpot Mystic's Shed	COIN
        shore	The Shore, Inc. Gift Shop	COIN
    """.trimIndent()

    private val sampleCoinmasters = """
        The Crackpot Mystic's Shed	ROW31	monster bait	red pixel (20)	white pixel (15)
        The Shore, Inc. Gift Shop	buy	3	dinghy plans	ROW176
        Bounty Hunter Hunter	buy	15	bounty-hunting rifle
    """.trimIndent()

    private fun registerSampleItems() {
        ItemDatabase.resetForTest()
        fun item(id: Int, name: String) = ItemData(
            id = id, name = name, descId = "0", image = "x.gif",
            primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
            access = emptySet(), autosellPrice = 0, plural = null,
        )
        ItemDatabase.registerForTest(item(1, "monster bait"))
        ItemDatabase.registerForTest(item(2, "red pixel"))
        ItemDatabase.registerForTest(item(3, "white pixel"))
        ItemDatabase.registerForTest(item(4, "dinghy plans"))
        ItemDatabase.registerForTest(item(3485, "bounty-hunting rifle"))
    }

    @Test
    fun loadFromText_parsesRowAndBuyLines() {
        CoinmasterDatabase.resetForTest()
        registerSampleItems()
        CoinmasterDatabase.loadFromText(sampleShops, sampleCoinmasters)

        val mystic = CoinmasterDatabase.findByNickname("mystic")
        assertNotNull(mystic)
        assertEquals("mystic", mystic.shopId)
        assertTrue(mystic.buyItems.any { it.rowId == 31 })

        val shore = CoinmasterDatabase.findByNickname("shore")
        assertNotNull(shore)
        assertTrue(shore.buyItems.any { it.rowId == 176 && it.price == 3 })

        val hunter = CoinmasterDatabase.findByNickname("hunter")
            ?: CoinmasterDatabase.findByNickname("bhh")
        assertNotNull(hunter)
        assertTrue(hunter.useItemField)
        assertEquals("bounty.php", hunter.buyUrl)
    }

    @Test
    fun parseItemToken_parsesQuantitySuffix() {
        registerSampleItems()
        val stack = CoinmasterDatabase.parseItemToken("red pixel (20)")
        assertNotNull(stack)
        assertEquals(20, stack.count)
        assertEquals(2, stack.itemId)
    }
}
