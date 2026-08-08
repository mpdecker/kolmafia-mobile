package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class SushiCreateRequestTest {

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

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
    fun create_beefyNigiri_postsSushiPhpAndRetrievesIngredients() = runTest {
        registerNigiriIngredients()
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val sushiPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("sushi.php") -> {
                    sushiPosts += request.body.toByteArray().decodeToString()
                    respond("You eat the beefy nigiri. Delicious!")
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = SushiCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { id, qty ->
                retrieved += id to qty
                qty
            },
            gameDatabase = null,
        )

        val result = request.create(beefyNigiriConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(
            listOf(
                BEEFY_FISH_ID to 1,
                WHITE_RICE_ID to 1,
                BEEFY_FISH_ID to 1,
                WHITE_RICE_ID to 1,
            ),
            retrieved,
        )
        assertEquals(1, sushiPosts.size)
        assertEquals("Yep.", formParam(sushiPosts.single(), "action"))
        assertEquals("1", formParam(sushiPosts.single(), "whichsushi"))
    }

    @Test
    fun create_success_registersSessionLogOnHappyPath() = runTest {
        registerNigiriIngredients()
        ConcoctionDatabase.injectForTest(beefyNigiriConcoction())
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("sushi.php") ->
                    respond("You eat the beefy nigiri. Delicious!")
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = SushiCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
            sessionLogger = sessionLogger,
            preferences = prefs,
        )

        val result = request.create(beefyNigiriConcoction(), 1, state = null, preferences = prefs)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertTrue(
            sessionLogger.recentLines().any {
                it.contains("Roll and eat beefy nigiri from 1 beefy fish meat, 1 white rice")
            },
        )
    }

    @Test
    fun create_tooFull_stopsPartialBatch() = runTest {
        registerNigiriIngredients()
        val client = HttpClient(MockEngine { respond("You are way too full to eat it right now.") })
        val request = SushiCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )

        val result = request.create(beefyNigiriConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        registerNigiriIngredients()
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = SushiCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )
        val prefs = Preferences(MapSettings())

        val result = request.create(
            beefyNigiriConcoction(),
            1,
            state = CharacterState(),
            preferences = prefs,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun create_unknownRecipe_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = SushiCreateRequest(
            client = client,
            createItemIngredients = createItemIngredients { _, qty -> qty },
            gameDatabase = null,
        )
        val concoction = ConcoctionData(
            result = "not sushi",
            resultQuantity = 1,
            methods = setOf("SUSHI"),
            ingredients = listOf(ConcoctionIngredient("beefy fish meat", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isFailure)
    }

    private fun beefyNigiriConcoction() = ConcoctionData(
        result = "beefy nigiri",
        resultQuantity = 1,
        methods = setOf("SUSHI"),
        ingredients = listOf(
            ConcoctionIngredient("beefy fish meat", 1),
            ConcoctionIngredient("white rice", 1),
        ),
    )

    private fun registerNigiriIngredients() {
        registerItem(BEEFY_FISH_ID, "beefy fish meat")
        registerItem(WHITE_RICE_ID, "white rice")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "beefy nigiri",
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 4,
                advMax = 8,
                muscMin = 8,
                muscMax = 16,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
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
        private const val BEEFY_FISH_ID = 89101
        private const val WHITE_RICE_ID = 89102
    }
}
