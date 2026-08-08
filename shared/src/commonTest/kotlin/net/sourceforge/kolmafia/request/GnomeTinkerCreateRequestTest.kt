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

class GnomeTinkerCreateRequestTest {

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
    fun create_clockworkWidget_postsGnomesAndRetrievesIngredients() = runTest {
        registerWidgetIngredients()
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val tinkerPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("gnomes.php") -> {
                    tinkerPosts += request.body.toByteArray().decodeToString()
                    respond("Gnorman deftly assembles your items into something new.")
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = GnomeTinkerCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(clockworkWidgetConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(
            listOf(
                FLANGE_ID to 1,
                COG_ID to 1,
                SPROCKET_ID to 1,
                FLANGE_ID to 1,
                COG_ID to 1,
                SPROCKET_ID to 1,
            ),
            retrieved,
        )
        assertEquals(1, tinkerPosts.size)
        assertEquals("tinker", formParam(tinkerPosts.single(), "place"))
        assertEquals("tinksomething", formParam(tinkerPosts.single(), "action"))
        assertEquals(FLANGE_ID.toString(), formParam(tinkerPosts.single(), "item1"))
        assertEquals(COG_ID.toString(), formParam(tinkerPosts.single(), "item2"))
        assertEquals(SPROCKET_ID.toString(), formParam(tinkerPosts.single(), "item3"))
        assertEquals("1", formParam(tinkerPosts.single(), "qty"))
    }

    @Test
    fun create_missingIngredients_stopsPartialBatch() = runTest {
        registerWidgetIngredients()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = GnomeTinkerCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                if (id == SPROCKET_ID) 0 else qty
            },
            gameDatabase = null,
        )

        val result = request.create(clockworkWidgetConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        registerWidgetIngredients()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = GnomeTinkerCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )
        val prefs = Preferences(MapSettings())

        val result = request.create(
            clockworkWidgetConcoction(),
            1,
            state = CharacterState(zodiacSign = "Wombat"),
            preferences = prefs,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun create_clockworkClockwiseDome_retrievesFlangeTwice() = runTest {
        registerWidgetIngredients()
        registerItem(WIDGET_ID, "clockwork widget")
        registerItem(SPRING_ID, "spring")
        val flangeRetrieves = mutableListOf<Int>()
        val tinkerPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("gnomes.php") -> {
                    tinkerPosts += request.body.toByteArray().decodeToString()
                    respond("Gnorman deftly assembles your items into something new.")
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = GnomeTinkerCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                if (id == FLANGE_ID) flangeRetrieves += qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(clockwiseDomeConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf(1, 1), flangeRetrieves)
        assertEquals(1, tinkerPosts.size)
        assertEquals(WIDGET_ID.toString(), formParam(tinkerPosts.single(), "item1"))
    }

    @Test
    fun create_invalidIngredientCount_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = GnomeTinkerCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )
        val concoction = ConcoctionData(
            result = "clockwork widget",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(
                ConcoctionIngredient("flange", 1),
                ConcoctionIngredient("cog", 1),
            ),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isFailure)
    }

    private fun clockwiseDomeConcoction() = ConcoctionData(
        result = "clockwork clockwise dome",
        resultQuantity = 1,
        methods = setOf("TINKER"),
        ingredients = listOf(
            ConcoctionIngredient("clockwork widget", 1),
            ConcoctionIngredient("flange", 1),
            ConcoctionIngredient("spring", 1),
        ),
    )

    private fun clockworkWidgetConcoction() = ConcoctionData(
        result = "clockwork widget",
        resultQuantity = 1,
        methods = setOf("TINKER"),
        ingredients = listOf(
            ConcoctionIngredient("flange", 1),
            ConcoctionIngredient("cog", 1),
            ConcoctionIngredient("sprocket", 1),
        ),
    )

    private fun registerWidgetIngredients() {
        registerItem(FLANGE_ID, "flange")
        registerItem(COG_ID, "cog")
        registerItem(SPROCKET_ID, "sprocket")
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

    companion object {
        private const val FLANGE_ID = 88301
        private const val COG_ID = 88302
        private const val SPROCKET_ID = 88303
        private const val WIDGET_ID = 88304
        private const val SPRING_ID = 88305
    }
}
