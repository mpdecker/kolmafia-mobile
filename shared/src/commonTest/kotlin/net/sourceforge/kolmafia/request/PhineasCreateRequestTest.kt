package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
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
import net.sourceforge.kolmafia.preferences.Preferences

class PhineasCreateRequestTest {

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
    fun create_sealhideHood_postsVolcanoislandAndRetrievesIngredients() = runTest {
        registerItem(HOOD_ID, "sealhide hood")
        registerItem(BRAIN_ID, "hellseal brain")
        registerItem(SINEW_ID, "hellseal sinew")
        registerItem(HIDE_ID, "hellseal hide")
        registerItem(WHISKER_ID, "hellseal whisker")
        registerItem(CLAW_ID, "hellseal claw")
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val phineasPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post &&
                    request.url.encodedPath.endsWith("volcanoisland.php") -> {
                    phineasPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>sealhide hood</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = PhineasCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(sealhideHoodConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(
            listOf(
                BRAIN_ID to 3, SINEW_ID to 2, HIDE_ID to 2, WHISKER_ID to 3, CLAW_ID to 5,
                BRAIN_ID to 3, SINEW_ID to 2, HIDE_ID to 2, WHISKER_ID to 3, CLAW_ID to 5,
            ),
            retrieved,
        )
        assertEquals(1, phineasPosts.size)
        assertEquals(HOOD_ID.toString(), formParam(phineasPosts.single(), "makewhich"))
        assertEquals("npc", formParam(phineasPosts.single(), "action"))
        assertEquals("make", formParam(phineasPosts.single(), "subaction"))
        assertEquals("1", formParam(phineasPosts.single(), "quantity"))
    }

    @Test
    fun create_missingIngredients_stopsPartialBatch() = runTest {
        registerItem(HOOD_ID, "sealhide hood")
        registerItem(BRAIN_ID, "hellseal brain")
        registerItem(SINEW_ID, "hellseal sinew")
        registerItem(HIDE_ID, "hellseal hide")
        registerItem(WHISKER_ID, "hellseal whisker")
        registerItem(CLAW_ID, "hellseal claw")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = PhineasCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                if (id == CLAW_ID) 0 else qty
            },
            gameDatabase = null,
        )

        val result = request.create(sealhideHoodConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        registerItem(HOOD_ID, "sealhide hood")
        registerItem(BRAIN_ID, "hellseal brain")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = PhineasCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )

        val result = request.create(
            sealhideHoodConcoction(),
            1,
            state = CharacterState(),
            preferences = Preferences(MapSettings()),
        )

        assertTrue(result.isFailure)
    }

    private fun sealhideHoodConcoction() = ConcoctionData(
        result = "sealhide hood",
        resultQuantity = 1,
        methods = setOf("PHINEAS"),
        ingredients = listOf(
            ConcoctionIngredient("hellseal brain", 3),
            ConcoctionIngredient("hellseal sinew", 2),
            ConcoctionIngredient("hellseal hide", 2),
            ConcoctionIngredient("hellseal whisker", 3),
            ConcoctionIngredient("hellseal claw", 5),
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
        private const val HOOD_ID = 88101
        private const val BRAIN_ID = 88102
        private const val SINEW_ID = 88103
        private const val HIDE_ID = 88104
        private const val WHISKER_ID = 88105
        private const val CLAW_ID = 88106
    }
}
