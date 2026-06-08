package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.HermitRequest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CLOVER_ITEM_ID = 24
private const val CLOVER_ITEM_NAME = "ten-leaf clover"

/** A HermitRequest subclass that records calls and returns a fixed result. */
private class FakeHermitRequest(
    private val tradeResult: Result<String>
) : HermitRequest(
    HttpClient(MockEngine {
        respond("unused", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
    })
) {
    val calls = mutableListOf<Pair<Int, Int>>()

    override suspend fun trade(itemId: Int, quantity: Int): Result<String> {
        calls.add(itemId to quantity)
        return tradeResult
    }
}

/** A minimal GameDatabase that resolves the clover item without loading from disk. */
private class HermitStubDatabase : GameDatabase() {
    private val cloverItem = ItemData(
        id = CLOVER_ITEM_ID,
        name = CLOVER_ITEM_NAME,
        descId = "hermit_desc",
        image = "clover.gif",
        primaryUse = ItemPrimaryUse.NONE,
        secondaryUses = emptySet(),
        access = setOf('t', 'd'),
        autosellPrice = 0,
        plural = "ten-leaf clovers"
    )

    override fun item(name: String): ItemData? =
        if (name.equals(CLOVER_ITEM_NAME, ignoreCase = true)) cloverItem else null

    override fun item(id: Int): ItemData? =
        if (id == CLOVER_ITEM_ID) cloverItem else null
}

class GameRuntimeLibraryHermitTest {

    // ── hermit(item, count) — item-first regression ────────────────

    @Test
    fun hermit_itemFirst_callsTradeAndReturnsCount() {
        val fake = FakeHermitRequest(Result.success("ok"))
        val lib = GameRuntimeLibrary(hermitRequest = fake, gameDatabase = HermitStubDatabase())
        val out = outputLib(lib, """print(to_string(hermit(to_item("ten-leaf clover"), 3)));""")
        assertEquals("3", out)
        assertEquals(listOf(CLOVER_ITEM_ID to 3), fake.calls)
    }

    @Test
    fun hermit_itemFirst_returnsZeroOnFailure() {
        val fake = FakeHermitRequest(Result.failure(Exception("network")))
        val lib = GameRuntimeLibrary(hermitRequest = fake, gameDatabase = HermitStubDatabase())
        val out = outputLib(lib, """print(to_string(hermit(to_item("ten-leaf clover"), 2)));""")
        assertEquals("0", out)
    }

    // ── hermit(count, item) — count-first (NEW overload) ──────────

    @Test
    fun hermit_countFirst_callsTradeAndReturnsCount() {
        val fake = FakeHermitRequest(Result.success("ok"))
        val lib = GameRuntimeLibrary(hermitRequest = fake, gameDatabase = HermitStubDatabase())
        val out = outputLib(lib, """print(to_string(hermit(2, to_item("ten-leaf clover"))));""")
        assertEquals("2", out)
        assertEquals(listOf(CLOVER_ITEM_ID to 2), fake.calls)
    }

    @Test
    fun hermit_countFirst_returnsZeroOnFailure() {
        val fake = FakeHermitRequest(Result.failure(Exception("network")))
        val lib = GameRuntimeLibrary(hermitRequest = fake, gameDatabase = HermitStubDatabase())
        val out = outputLib(lib, """print(to_string(hermit(1, to_item("ten-leaf clover"))));""")
        assertEquals("0", out)
    }

    @Test
    fun hermit_countFirst_returnsZeroWhenCountIsZero() {
        val fake = FakeHermitRequest(Result.success("ok"))
        val lib = GameRuntimeLibrary(hermitRequest = fake, gameDatabase = HermitStubDatabase())
        val out = outputLib(lib, """print(to_string(hermit(0, to_item("ten-leaf clover"))));""")
        assertEquals("0", out)
        assertEquals(emptyList<Pair<Int, Int>>(), fake.calls, "Should not call trade when count is 0")
    }
}
