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
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun okClient(): HttpClient = HttpClient(MockEngine {
    respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
})

private class DisplayStashDb : GameDatabase() {
    private val gem = ItemData(55, "brilliant gem", "desc", "gem.gif",
        ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
    override fun item(name: String): ItemData? =
        if (name.equals("brilliant gem", ignoreCase = true)) gem else null
    override fun item(id: Int): ItemData? = if (id == 55) gem else null
}

class GameRuntimeLibraryDisplayStashTest {

    // ── put_display ────────────────────────────────────────────────────────────

    @Test
    fun putDisplay_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            displayCaseRequest = DisplayCaseRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(put_display(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun putDisplay_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), displayCaseRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(put_display(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun putDisplay_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            displayCaseRequest = DisplayCaseRequest(okClient())
        )
        assertEquals("false",
            outputLib(lib, """print(to_string(put_display(1, to_item("no such item"))));"""))
    }

    // ── take_display ───────────────────────────────────────────────────────────

    @Test
    fun takeDisplay_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            displayCaseRequest = DisplayCaseRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(take_display(2, to_item("brilliant gem"))));"""))
    }

    @Test
    fun takeDisplay_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), displayCaseRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(take_display(1, to_item("brilliant gem"))));"""))
    }

    // ── put_stash ──────────────────────────────────────────────────────────────

    @Test
    fun putStash_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            clanStashRequest = ClanStashRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(put_stash(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun putStash_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), clanStashRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(put_stash(1, to_item("brilliant gem"))));"""))
    }

    // ── take_stash ─────────────────────────────────────────────────────────────

    @Test
    fun takeStash_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = DisplayStashDb(),
            clanStashRequest = ClanStashRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(take_stash(1, to_item("brilliant gem"))));"""))
    }

    @Test
    fun takeStash_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = DisplayStashDb(), clanStashRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(take_stash(1, to_item("brilliant gem"))));"""))
    }
}
