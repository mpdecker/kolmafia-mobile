package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.EatFoodRequest

class GameRuntimeLibraryAshP555Test {

    private fun itemData(id: Int, name: String) = ItemData(
        id = id,
        name = name,
        descId = "",
        image = "",
        primaryUse = ItemPrimaryUse.FOOD,
        secondaryUses = emptySet(),
        access = setOf('t'),
        autosellPrice = 0,
        plural = null,
    )

    private fun db(vararg items: Pair<String, Int>) = object : GameDatabase() {
        override fun item(name: String) = items.firstOrNull { it.first.equals(name, ignoreCase = true) }
            ?.let { itemData(it.second, it.first) }
        override fun item(id: Int) = items.firstOrNull { it.second == id }
            ?.let { itemData(it.second, it.first) }
    }

    @Test
    fun revision_phase556() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun eatsilent_qtyOptional_routesToEat() {
        var eaten = 0
        val eat = object : EatFoodRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun eat(itemId: Int, quantity: Int): Result<String> {
                eaten += quantity
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(gameDatabase = db("toast" to 282), eatFoodRequest = eat)
        outputLib(lib, """cli_execute("eatsilent toast");""")
        assertEquals(1, eaten)
        eaten = 0
        outputLib(lib, """cli_execute("eatsilent 2 toast");""")
        assertEquals(2, eaten)
    }

    @Test
    fun overdrink_and_drinksilent_qtyOptional_promptWhenEmpty() {
        val over = outputLib(GameRuntimeLibrary(), """cli_execute("overdrink");""")
        assertTrue(over.contains("drink", ignoreCase = true))
        val silent = outputLib(GameRuntimeLibrary(), """cli_execute("drinksilent");""")
        assertTrue(silent.contains("drink", ignoreCase = true))
    }

    @Test
    fun overdrink_withItem_doesNotThrow() {
        val lib = GameRuntimeLibrary(gameDatabase = db("sake" to 1548))
        outputLib(lib, """cli_execute("overdrink sake");""")
        outputLib(lib, """cli_execute("overdrink 2 sake");""")
        outputLib(lib, """cli_execute("drinksilent sake");""")
    }
}
