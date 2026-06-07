package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A minimal GameDatabase that returns a single fake item without loading from disk. */
private class StubGameDatabase(
    private val fakeItemName: String,
    private val fakeItemId: Int
) : GameDatabase() {
    private val fakeItem = ItemData(
        id = fakeItemId,
        name = fakeItemName,
        descId = "test_desc",
        image = "test.gif",
        primaryUse = ItemPrimaryUse.NONE,
        secondaryUses = emptySet(),
        access = setOf('t', 'd'),
        autosellPrice = 10,
        plural = null
    )

    override fun item(name: String): ItemData? =
        if (name.equals(fakeItemName, ignoreCase = true)) fakeItem else null

    override fun item(id: Int): ItemData? =
        if (id == fakeItemId) fakeItem else null
}

private fun makeClient(handler: MockRequestHandler): HttpClient =
    HttpClient(MockEngine(handler))

private fun okClient(): HttpClient = makeClient {
    respond(
        content = "ok",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "text/html")
    )
}

private fun stubDb(itemName: String = "test item", itemId: Int = 42) =
    StubGameDatabase(itemName, itemId)

class GameRuntimeLibraryItemActionsTest {

    // ── use() ──────────────────────────────────────────────────────

    @Test
    fun use_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            useItemRequest = UseItemRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(use(1, to_item("test item"))));"""))
    }

    @Test
    fun use_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            useItemRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(use(1, to_item("test item"))));"""))
    }

    @Test
    fun use_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            useItemRequest = UseItemRequest(okClient())
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(use(1, to_item("no such item"))));"""))
    }

    // ── eat() ──────────────────────────────────────────────────────

    @Test
    fun eat_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            eatFoodRequest = EatFoodRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(eat(1, to_item("test item"))));"""))
    }

    @Test
    fun eat_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            eatFoodRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(eat(1, to_item("test item"))));"""))
    }

    @Test
    fun eat_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            eatFoodRequest = EatFoodRequest(okClient())
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(eat(1, to_item("no such item"))));"""))
    }

    // ── drink() ────────────────────────────────────────────────────

    @Test
    fun drink_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            drinkBoozeRequest = DrinkBoozeRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(drink(1, to_item("test item"))));"""))
    }

    @Test
    fun drink_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            drinkBoozeRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(drink(1, to_item("test item"))));"""))
    }

    // ── chew() ─────────────────────────────────────────────────────

    @Test
    fun chew_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            chewRequest = ChewRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(chew(1, to_item("test item"))));"""))
    }

    @Test
    fun chew_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            chewRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(chew(1, to_item("test item"))));"""))
    }

    // ── autosell() ─────────────────────────────────────────────────

    @Test
    fun autosell_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            autosellRequest = AutosellRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(autosell(3, to_item("test item"))));"""))
    }

    @Test
    fun autosell_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            autosellRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(autosell(1, to_item("test item"))));"""))
    }

    // ── put_closet() ───────────────────────────────────────────────

    @Test
    fun putCloset_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            closetRequest = ClosetRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(put_closet(2, to_item("test item"))));"""))
    }

    @Test
    fun putCloset_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            closetRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(put_closet(1, to_item("test item"))));"""))
    }

    // ── take_closet() ──────────────────────────────────────────────

    @Test
    fun takeCloset_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            closetRequest = ClosetRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(take_closet(1, to_item("test item"))));"""))
    }

    @Test
    fun takeCloset_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            closetRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(take_closet(1, to_item("test item"))));"""))
    }

    // ── take_storage() ─────────────────────────────────────────────

    @Test
    fun takeStorage_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            storageRequest = StorageRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(take_storage(1, to_item("test item"))));"""))
    }

    @Test
    fun takeStorage_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            storageRequest = null
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(take_storage(1, to_item("test item"))));"""))
    }

    // ── put_shop() — stub ──────────────────────────────────────────

    @Test
    fun putShop_alwaysReturnsFalse() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false",
            outputLib(lib, """print(to_string(put_shop(100, 0, to_item("anything"))));"""))
    }
}
