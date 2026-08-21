package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest

class GameRuntimeLibraryAshP529Test {

    private class RecordingMall : MallManager(
        MallSearchRequest(HttpClient(MockEngine { respond("") })),
        MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
        null,
    ) {
        val buys = mutableListOf<Triple<Int, Int, Int>>()
        override suspend fun cheapestPrice(itemName: String): Long = 100
        override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
            buys += Triple(itemId, count, maxPrice)
            return count
        }
    }

    @BeforeTest
    fun setUp() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 2,
                name = "seal tooth",
                descId = "d2",
                image = "tooth.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase529() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun buy_usingStorage_named_buysWhenRestricted() {
        val mall = RecordingMall()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "1", adventures = "5"))
        val out = outputLib(
            GameRuntimeLibrary(character = char, mallManager = mall),
            """cli_execute("buy using storage 2 seal tooth");""",
        )
        assertEquals(listOf(Triple(2, 2, Int.MAX_VALUE)), mall.buys)
        assertFalse(out.contains("cannot purchase using storage"))
    }

    @Test
    fun buy_usingStorage_named_errorsWhenCanInteract() {
        val mall = RecordingMall()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hardcore = "0", roninleft = "0"))
        val out = outputLib(
            GameRuntimeLibrary(character = char, mallManager = mall),
            """cli_execute("buy using storage seal tooth");""",
        )
        assertTrue(out.contains("You cannot purchase using storage unless you are in Hardcore or Ronin"))
        assertTrue(mall.buys.isEmpty())
    }
}
