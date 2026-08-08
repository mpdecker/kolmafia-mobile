package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

class TerminalExtrudeCreateRequestTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    @Test
    fun create_retrievesEssenceAndExtrudes() = runTest {
        registerItem(9034, "Source essence")
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val client = HttpClient(MockEngine { respond("You acquire an item: <b>browser cookie</b>", HttpStatusCode.OK) })
        val terminalRequest = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val request = TerminalExtrudeCreateRequest(
            terminalRequest = terminalRequest,
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { id, qty ->
                    retrieved += id to qty
                    qty
                },
                gameDatabase = null,
            ),
        )
        val concoction = ConcoctionData(
            result = "browser cookie",
            resultQuantity = 1,
            methods = setOf("TERMINAL"),
            ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_campgroundHasSourceTerminal", true)

        val result = request.create(concoction, 1, state = null, prefs)

        assertTrue(result.isSuccess)
        assertEquals(listOf(9034 to 10, 9034 to 10), retrieved)
        assertEquals(1, prefs.getInt("_sourceTerminalExtrudes", 0))
    }

    @Test
    fun create_missingAcquireText_returnsFailure() = runTest {
        registerItem(9034, "Source essence")
        val client = HttpClient(MockEngine { respond("nothing happened", HttpStatusCode.OK) })
        val terminalRequest = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val request = TerminalExtrudeCreateRequest(
            terminalRequest = terminalRequest,
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService(),
                gameDatabase = null,
            ),
        )
        val concoction = ConcoctionData(
            result = "browser cookie",
            resultQuantity = 1,
            methods = setOf("TERMINAL"),
            ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_campgroundHasSourceTerminal", true)

        val result = request.create(concoction, 1, state = null, prefs)

        assertTrue(result.isFailure)
        assertEquals(0, prefs.getInt("_sourceTerminalExtrudes", 0))
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        registerItem(9034, "Source essence")
        val client = HttpClient(MockEngine { respond("You acquire an item.", HttpStatusCode.OK) })
        val terminalRequest = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val request = TerminalExtrudeCreateRequest(
            terminalRequest = terminalRequest,
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService(),
                gameDatabase = null,
            ),
        )
        val concoction = ConcoctionData(
            result = "browser cookie",
            resultQuantity = 1,
            methods = setOf("TERMINAL"),
            ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("_sourceTerminalExtrudes", 3)

        val result = request.create(
            concoction,
            1,
            CharacterState(),
            prefs,
            accessibleCount = { id -> if (id == 9033) 1 else 0 },
        )

        assertTrue(result.isFailure)
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }
}
