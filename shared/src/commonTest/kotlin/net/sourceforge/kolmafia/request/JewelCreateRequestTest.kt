package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
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

class JewelCreateRequestTest {

    private fun createItemIngredients(
        retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ): CreateItemIngredients = CreateItemIngredients(StubRetrieveItemService(retrieveFn), null)

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    private fun formParam(body: String, key: String): String? =
        Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)

    @Test
    fun create_hamethystRing_postsCraftCombine() = runTest {
        registerItem(HAMYST_ID, "hamethyst")
        registerItem(SETTING_ID, "ring setting")
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val craftPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("craft.php") -> {
                    craftPosts += request.body.toByteArray().decodeToString()
                    respond("<!-- cr:1x0,0=88501 -->", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = JewelCreateRequest(
            craftRequest = CraftRequest(client),
            createItemIngredients = createItemIngredients { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(hamethystRingConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf(HAMYST_ID to 1, SETTING_ID to 1, HAMYST_ID to 1, SETTING_ID to 1), retrieved)
        assertEquals(1, craftPosts.size)
        assertEquals("combine", formParam(craftPosts.single(), "mode"))
        assertEquals(HAMYST_ID.toString(), formParam(craftPosts.single(), "a"))
        assertEquals(SETTING_ID.toString(), formParam(craftPosts.single(), "b"))
    }

    @Test
    fun create_missingIngredients_stopsPartialBatch() = runTest {
        registerItem(HAMYST_ID, "hamethyst")
        registerItem(SETTING_ID, "ring setting")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = JewelCreateRequest(
            craftRequest = CraftRequest(client),
            createItemIngredients = createItemIngredients { _, _ -> 0 },
            gameDatabase = null,
        )

        val result = request.create(hamethystRingConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermittedWithoutPliers_returnsFailure() = runTest {
        registerItem(HAMYST_ID, "hamethyst")
        registerItem(SETTING_ID, "ring setting")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = JewelCreateRequest(
            craftRequest = CraftRequest(client),
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
            accessibleCount = { 0 },
        )
        val state = CharacterState()

        val result = request.create(hamethystRingConcoction(), 1, state = state, preferences = null)

        assertTrue(result.isFailure)
    }

    private fun hamethystRingConcoction() = ConcoctionData(
        result = "hamethyst ring",
        resultQuantity = 1,
        methods = setOf("JEWEL"),
        ingredients = listOf(
            ConcoctionIngredient("hamethyst", 1),
            ConcoctionIngredient("ring setting", 1),
        ),
    )

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

    companion object {
        private const val HAMYST_ID = 88501
        private const val SETTING_ID = 88502
    }
}
