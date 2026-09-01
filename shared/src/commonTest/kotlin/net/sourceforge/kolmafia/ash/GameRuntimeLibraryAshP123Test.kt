package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameRuntimeLibraryAshP123Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun isNpcItem_trueWhenInNpcStore() {
        registerItem(9301, "npc probe item")
        NpcStoreDatabase.loadFromText("Probe Shop\tprobes\tnpc probe item\t50\n")
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(is_npc_item(to_item("npc probe item")));""").trim(),
        )
    }

    @Test
    fun isCoinmasterItem_trueWhenBuyRowExists() {
        registerItem(9302, "coin probe item")
        CoinmasterDatabase.loadFromText(
            shopsText = "coinprobe\tCoin Probe\n",
            coinText = "Coin Probe\tbuy\t100\tcoin probe item\tROW9302\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(to_item("coin probe item")));""").trim(),
        )
    }

    @Test
    fun concoctionPrice_sumsIngredientMallPricesPlusCreationCost() {
        registerItem(9310, "priced result")
        registerItem(9311, "priced part a")
        registerItem(9312, "priced part b")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "priced result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("priced part a", 1),
                    ConcoctionIngredient("priced part b", 2),
                ),
            ),
        )
        val mall = object : MallManager(
            MallSearchRequest(HttpClient(MockEngine { respond("[]") })),
            MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
            null,
        ) {
            override suspend fun cheapestPrice(itemName: String): Long = when (itemName) {
                "priced part a" -> 100L
                "priced part b" -> 50L
                else -> -1L
            }
        }
        val lib = GameRuntimeLibrary(mallManager = mall)
        assertEquals("210", outputLib(lib, """print(concoction_price(to_item("priced result")));""").trim())
    }

    @Test
    fun revision_isphase170() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
