package net.sourceforge.kolmafia.shop

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class CoinmasterDatabaseContainsTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun containsBuyItem_trueWhenBuyRowExists() {
        ItemDatabase.registerForTest(testItem(9101, "coin widget"))
        CoinmasterDatabase.loadFromText(
            shopsText = "testcoin\tTest Coinmaster\n",
            coinText = "Test Coinmaster\tbuy\t100\tcoin widget\tROW9101\n",
        )
        assertTrue(CoinmasterDatabase.containsBuyItem(9101))
    }

    @Test
    fun containsBuyItem_falseWhenMissing() {
        assertFalse(CoinmasterDatabase.containsBuyItem(9999))
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
