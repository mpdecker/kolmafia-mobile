package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.ShopRequest

class StillCreateRequestTest {

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int,
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    @Test
    fun create_postsStillShopBuyAndReturnsSuccess() = runTest {
        val ginId = 93001
        registerItem(ginId, "bottle of gin")
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond(
                "<html><body>You acquire an item: <b>Calcutta Emerald</b>. with 4 bright stills.</body></html>",
                HttpStatusCode.OK,
            )
        })
        val character = KoLCharacter()
        val retrieve = StubRetrieveItemService { id, qty ->
            assertEquals(ginId, id)
            assertEquals(2, qty)
            qty
        }
        val request = StillCreateRequest(
            shopRequest = ShopRequest(client),
            createItemIngredients = CreateItemIngredients(retrieve, null),
            gameDatabase = null,
            character = character,
        )
        val concoction = ConcoctionData(
            result = "bottle of Calcutta Emerald",
            resultQuantity = 1,
            methods = setOf("STILL", "ROW267"),
            ingredients = listOf(ConcoctionIngredient("bottle of gin", 1)),
        )

        val result = request.create(
            concoction = concoction,
            quantity = 2,
            state = CharacterState(stillsAvailable = 1),
            preferences = Preferences(MapSettings()),
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
        val body = bodies.single()
        assertTrue(body.contains("whichshop=still"), body)
        assertTrue(body.contains("whichrow=267"), body)
        assertTrue(body.contains("quantity=2"), body)
        assertEquals(4, character.state.value.stillsAvailable)
    }

    @Test
    fun create_missingAcquireText_returnsFailure() = runTest {
        val ginId = 93002
        registerItem(ginId, "test gin")
        val client = HttpClient(MockEngine { respond("nothing happened", HttpStatusCode.OK) })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val request = StillCreateRequest(
            shopRequest = ShopRequest(client),
            createItemIngredients = CreateItemIngredients(retrieve, null),
            gameDatabase = null,
            character = null,
        )
        val concoction = ConcoctionData(
            result = "test still booze",
            resultQuantity = 1,
            methods = setOf("STILL", "ROW100"),
            ingredients = listOf(ConcoctionIngredient("test gin", 1)),
        )

        val result = request.create(concoction, 1, CharacterState(stillsAvailable = 1), null)

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
