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
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest

class GameRuntimeLibraryAshP129Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun concoctionPrice_vykea_sumsIngredientMallPrices() {
        registerItem(8729, "VYKEA hex key")
        registerItem(8730, "VYKEA instructions")
        registerItem(8725, "VYKEA plank")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "level 1 couch",
                resultQuantity = 1,
                methods = setOf("VYKEA"),
                ingredients = listOf(
                    ConcoctionIngredient("VYKEA instructions", 1),
                    ConcoctionIngredient("VYKEA plank", 10),
                ),
            ),
        )
        val mall = object : MallManager(
            MallSearchRequest(HttpClient(MockEngine { respond("[]") })),
            MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
            null,
        ) {
            override suspend fun cheapestPrice(itemName: String): Long = when {
                itemName.equals("VYKEA instructions", ignoreCase = true) -> 111L
                itemName.equals("VYKEA plank", ignoreCase = true) -> 5L
                else -> -1L
            }
        }
        val lib = GameRuntimeLibrary(mallManager = mall)
        assertEquals(
            "161",
            outputLib(lib, """print(concoction_price(to_vykea("level 1 couch")));""").trim(),
        )
    }

    @Test
    fun revision_isphase170() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
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
