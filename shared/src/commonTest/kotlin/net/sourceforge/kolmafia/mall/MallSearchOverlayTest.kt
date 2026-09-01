package net.sourceforge.kolmafia.mall

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopRow

class MallSearchOverlayTest {
    @BeforeTest
    fun loadStores() {
        NpcStoreDatabase.loadFromText(
            """
            The General Store	generalstore	Meat paste	50
            """.trimIndent(),
        )
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Test Coinmaster",
                nickname = "testcoin",
                shopId = "testshop",
                token = null,
                buyItems = listOf(
                    ShopRow(
                        rowId = 1,
                        item = ItemStack(itemId = 99001, count = 1),
                        price = 12,
                    ),
                ),
                sellItems = emptyList(),
            ),
        )
        ItemDatabase.registerItem(99001, "test coin item", "99001")
    }

    @Test
    fun merge_prependsNpcAndCoinmasterRows() {
        val mallRows = listOf(
            MallListing(111, "Mall Shop", 42, 125, 5),
        )
        val merged = MallSearchOverlay.merge(
            searchString = "\"test coin item\"",
            mallRows = mallRows,
            limit = 10,
        )

        assertTrue(merged.any { it.source == MallListingSource.COINMASTER && it.itemId == 99001 })
        assertTrue(merged.any { it.source == MallListingSource.MALL && it.shopId == 111 })
        assertEquals(MallSearchOverlay.COINMASTER_SHOP_ID, merged.first { it.itemId == 99001 }.shopId)
    }

    @Test
    fun resolveSearchItemIds_parsesQuotedExactName() {
        ItemDatabase.registerItem(777, "widget of testing", "777")
        assertEquals(setOf(777), MallSearchOverlay.resolveSearchItemIds("\"widget of testing\""))
    }
}
