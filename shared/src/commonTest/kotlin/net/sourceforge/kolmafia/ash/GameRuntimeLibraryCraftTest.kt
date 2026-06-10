package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.CraftRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCraftTest {

    private fun stubDb(): GameDatabase = object : GameDatabase() {
        override fun item(id: Int) = when (id) {
            42 -> ItemData(
                id = 42, name = "present", descId = "", image = "",
                primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                access = setOf('t'), autosellPrice = 10, plural = null
            )
            1 -> ItemData(
                id = 1, name = "box", descId = "", image = "",
                primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                access = setOf('t'), autosellPrice = 10, plural = null
            )
            2 -> ItemData(
                id = 2, name = "wrapping paper", descId = "", image = "",
                primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                access = setOf('t'), autosellPrice = 10, plural = null
            )
            else -> null
        }
        override fun item(name: String) = when (name.lowercase()) {
            "present" -> item(42)
            "box" -> item(1)
            "paper", "wrapping paper" -> item(2)
            else -> null
        }
    }

    @Test
    fun create_delegatesToRetrieveItemService() {
        val lib = GameRuntimeLibrary(
            gameDatabase = stubDb(),
            retrieveItemService = object : RetrieveItemService(null, null, null, null, null, null, null, null, null, null, stubDb()) {
                override suspend fun retrieve(itemId: Int, qty: Int) = if (itemId == 42) qty else 0
            },
        )
        assertEquals("true", outputLib(lib, """print(to_string(create(2, to_item("present"))));"""))
    }

    @Test
    fun craft_returnsCreatedCount() {
        val lib = GameRuntimeLibrary(
            craftRequest = CraftRequest(HttpClient(MockEngine {
                respond("<!-- cr:3x1,2=9 -->", status = io.ktor.http.HttpStatusCode.OK)
            })),
            gameDatabase = stubDb(),
        )
        assertEquals("3", outputLib(lib, """print(to_string(craft("combine", 3, to_item("box"), to_item("paper"))));"""))
    }
}
