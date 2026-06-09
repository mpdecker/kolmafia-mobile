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
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun okClient(): HttpClient = HttpClient(MockEngine {
    respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
})

/**
 * Two-item stub (food + booze) covering both consumption categories.
 * GameRuntimeLibraryItemActionsTest.kt has a single-item StubGameDatabase
 * that covers only one item at a time; this variant is purpose-built for
 * tests that need both a food and a booze item in scope simultaneously.
 */
private class StubDb : GameDatabase() {
    private val food = ItemData(1001, "magical sausage", "desc", "food.gif",
        ItemPrimaryUse.FOOD, emptySet(), setOf('t', 'd'), 10, null)
    private val booze = ItemData(1002, "bottle of gin", "desc", "gin.gif",
        ItemPrimaryUse.DRINK, emptySet(), setOf('t', 'd'), 20, null)
    override fun item(name: String): ItemData? = when {
        name.equals("magical sausage", ignoreCase = true) -> food
        name.equals("bottle of gin", ignoreCase = true) -> booze
        else -> null
    }
    override fun item(id: Int): ItemData? = when (id) { 1001 -> food; 1002 -> booze; else -> null }
}

class GameRuntimeLibraryConsumptionTest {

    // ── eatsilent ──────────────────────────────────────────────────────────────

    @Test
    fun eatsilent_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb(),
            eatFoodRequest = EatFoodRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(eatsilent(1, to_item("magical sausage"))));"""))
    }

    @Test
    fun eatsilent_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), eatFoodRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(eatsilent(1, to_item("magical sausage"))));"""))
    }

    @Test
    fun eatsilent_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), eatFoodRequest = EatFoodRequest(okClient()))
        assertEquals("false",
            outputLib(lib, """print(to_string(eatsilent(1, to_item("mystery meat"))));"""))
    }

    // ── drinksilent ────────────────────────────────────────────────────────────

    @Test
    fun drinksilent_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb(),
            drinkBoozeRequest = DrinkBoozeRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(drinksilent(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun drinksilent_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(drinksilent(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun drinksilent_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = DrinkBoozeRequest(okClient()))
        assertEquals("false",
            outputLib(lib, """print(to_string(drinksilent(1, to_item("mystery booze"))));"""))
    }

    // ── overdrink ──────────────────────────────────────────────────────────────

    @Test
    fun overdrink_returnsTrueOnSuccess() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb(),
            drinkBoozeRequest = DrinkBoozeRequest(okClient())
        )
        assertEquals("true",
            outputLib(lib, """print(to_string(overdrink(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun overdrink_returnsFalseWithNullRequest() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = null)
        assertEquals("false",
            outputLib(lib, """print(to_string(overdrink(1, to_item("bottle of gin"))));"""))
    }

    @Test
    fun overdrink_returnsFalseForUnknownItem() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb(), drinkBoozeRequest = DrinkBoozeRequest(okClient()))
        assertEquals("false",
            outputLib(lib, """print(to_string(overdrink(1, to_item("mystery booze"))));"""))
    }
}
