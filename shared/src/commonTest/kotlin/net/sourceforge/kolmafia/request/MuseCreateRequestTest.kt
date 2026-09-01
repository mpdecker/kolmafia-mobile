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
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class MuseCreateRequestTest {

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
    fun create_potteryYoYo_postsMultiUse() = runTest {
        registerItem(POTSHERD_ID, "smoked potsherd")
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val multiUsePosts = mutableListOf<Pair<Int, Int>>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("multiuse.php") -> {
                    val body = request.body.toByteArray().decodeToString()
                    val itemId = formParam(body, "whichitem")?.toIntOrNull() ?: -1
                    val qty = formParam(body, "quantity")?.toIntOrNull() ?: -1
                    multiUsePosts += itemId to qty
                    respond("You acquire an item: <b>pottery yo-yo</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = MuseCreateRequest(
            useItemRequest = UseItemRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { id, qty ->
                    retrieved += id to qty
                    qty
                },
                gameDatabase = null,
            ),
            gameDatabase = null,
        )

        val result = request.create(potteryYoYoConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf(POTSHERD_ID to 5, POTSHERD_ID to 5), retrieved)
        assertEquals(listOf(POTSHERD_ID to 5), multiUsePosts)
    }

    @Test
    fun create_brickoBat_retrievesBothIngredients() = runTest {
        registerItem(BRICK_ID, "BRICKO brick")
        registerItem(EYE_BRICK_ID, "BRICKO eye brick")
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("multiuse.php") -> {
                    respond("You acquire an item: <b>BRICKO bat</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val request = MuseCreateRequest(
            useItemRequest = UseItemRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { id, qty ->
                    retrieved += id to qty
                    qty
                },
                gameDatabase = null,
            ),
            gameDatabase = null,
        )

        val result = request.create(brickoBatConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertTrue(retrieved.contains(BRICK_ID to 5))
        assertTrue(retrieved.contains(EYE_BRICK_ID to 1))
    }

    @Test
    fun create_qtyOne_usesInvUse() = runTest {
        registerItem(SINGLE_ID, "single-use muse item")
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respond("You acquire an item: <b>muse result</b>", HttpStatusCode.OK)
        })
        val request = MuseCreateRequest(
            useItemRequest = UseItemRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { _, qty -> qty },
                gameDatabase = null,
            ),
            gameDatabase = null,
        )
        val concoction = ConcoctionData(
            result = "muse result",
            resultQuantity = 1,
            methods = setOf("MUSE"),
            ingredients = listOf(ConcoctionIngredient("single-use muse item", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(listOf("/inv_use.php"), paths)
    }

    @Test
    fun create_missingIngredients_stopsPartialBatch() = runTest {
        registerItem(POTSHERD_ID, "smoked potsherd")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = MuseCreateRequest(
            useItemRequest = UseItemRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { _, qty -> if (qty == 5) 0 else qty },
                gameDatabase = null,
            ),
            gameDatabase = null,
        )

        val result = request.create(potteryYoYoConcoction(), 2, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        registerItem(POTSHERD_ID, "smoked potsherd")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val request = MuseCreateRequest(
            useItemRequest = UseItemRequest(client),
            createItemIngredients = CreateItemIngredients(
                StubRetrieveItemService { _, qty -> qty },
                gameDatabase = null,
            ),
            gameDatabase = null,
        )
        val concoction = ConcoctionData(
            result = "pottery yo-yo",
            resultQuantity = 1,
            methods = setOf("MANUAL", "MUSE"),
            ingredients = listOf(ConcoctionIngredient("smoked potsherd", 5)),
        )

        val result = request.create(
            concoction,
            1,
            state = CharacterState(),
            preferences = Preferences(MapSettings()),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun create_palmFrondFan_postsMultiUseAndAccountsOnce() = runTest {
        registerItem(PALM_FROND_ID, "palm frond")
        registerItem(PALM_FROND_FAN_ID, "palm-frond fan")
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            bodies += request.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>palm-frond fan</b>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val inventory = inventory(
            client,
            PALM_FROND_ID to 5,
            PALM_FROND_FAN_ID to 0,
        )
        val request = museRequest(client, inventory, preferences)

        val result = request.create(palmFrondFanConcoction(), 1, state = null, preferences = preferences)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(1, result.getOrThrow())
        assertEquals(listOf("/multiuse.php"), paths)
        assertEquals("useitem", formParam(bodies.single(), "action"))
        assertEquals(PALM_FROND_ID.toString(), formParam(bodies.single(), "whichitem"))
        assertEquals("2", formParam(bodies.single(), "quantity"))
        assertEquals(3, inventory.getCount(PALM_FROND_ID))
        assertEquals(1, inventory.getCount(PALM_FROND_FAN_ID))
        assertTrue(preferences.getString(SessionLogger.SESSION_LOG_KEY, "").contains("Use 2 palm frond"))
    }

    @Test
    fun create_singleUsePalmFrond_postsInvUseAndAccountsOnce() = runTest {
        registerItem(PALM_FROND_ID, "palm frond")
        registerItem(PALM_FROND_TOKEN_ID, "palm-frond token")
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respond("You acquire an item: <b>palm-frond token</b>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val inventory = inventory(client, PALM_FROND_ID to 2, PALM_FROND_TOKEN_ID to 0)
        val request = museRequest(client, inventory, preferences)
        val concoction = ConcoctionData(
            result = "palm-frond token",
            resultQuantity = 1,
            methods = setOf("MUSE"),
            ingredients = listOf(ConcoctionIngredient("palm frond", 1)),
        )

        val result = request.create(concoction, 1, state = null, preferences = preferences)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf("/inv_use.php"), paths)
        assertEquals(1, inventory.getCount(PALM_FROND_ID))
        assertEquals(1, inventory.getCount(PALM_FROND_TOKEN_ID))
        assertTrue(preferences.getString(SessionLogger.SESSION_LOG_KEY, "").contains("Use 1 palm frond"))
    }

    @Test
    fun create_palmFrondRetrievalFailure_issuesNoHttpAndPreservesInventory() = runTest {
        registerItem(PALM_FROND_ID, "palm frond")
        registerItem(PALM_FROND_FAN_ID, "palm-frond fan")
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("You acquire an item: <b>palm-frond fan</b>", HttpStatusCode.OK)
        })
        val inventory = inventory(client, PALM_FROND_ID to 4, PALM_FROND_FAN_ID to 0)
        val request = museRequest(
            client,
            inventory,
            retrieveFn = { _, _ -> 0 },
        )

        val result = request.create(palmFrondFanConcoction(), 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
        assertEquals(0, calls)
        assertEquals(4, inventory.getCount(PALM_FROND_ID))
        assertEquals(0, inventory.getCount(PALM_FROND_FAN_ID))
    }

    @Test
    fun create_malformedPalmFrondResponse_preservesInventoryAndSkipsSessionLog() = runTest {
        registerItem(PALM_FROND_ID, "palm frond")
        registerItem(PALM_FROND_FAN_ID, "palm-frond fan")
        val client = HttpClient(MockEngine {
            respond("<html>nothing interesting happens</html>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val inventory = inventory(client, PALM_FROND_ID to 4, PALM_FROND_FAN_ID to 0)
        val request = museRequest(client, inventory, preferences)

        val result = request.create(palmFrondFanConcoction(), 1, state = null, preferences = preferences)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
        assertEquals(4, inventory.getCount(PALM_FROND_ID))
        assertEquals(0, inventory.getCount(PALM_FROND_FAN_ID))
        assertEquals("", preferences.getString(SessionLogger.SESSION_LOG_KEY, ""))
    }

    @Test
    fun create_palmFrondSuccessAccounting_isOncePerSignature() = runTest {
        registerItem(PALM_FROND_ID, "palm frond")
        registerItem(PALM_FROND_FAN_ID, "palm-frond fan")
        val html = "You acquire an item: <b>palm-frond fan</b>"
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val inventory = inventory(client, PALM_FROND_ID to 6, PALM_FROND_FAN_ID to 0)
        val request = museRequest(client, inventory)
        val url = "multiuse.php?action=useitem&whichitem=$PALM_FROND_ID&quantity=2"

        request.create(palmFrondFanConcoction(), 1, state = null, preferences = null)
        request.parseResponse(url, html, palmFrondFanConcoction())

        assertEquals(4, inventory.getCount(PALM_FROND_ID))
        assertEquals(1, inventory.getCount(PALM_FROND_FAN_ID))
    }

    private fun museRequest(
        client: HttpClient,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) = MuseCreateRequest(
        useItemRequest = UseItemRequest(client),
        createItemIngredients = CreateItemIngredients(
            StubRetrieveItemService(retrieveFn),
            gameDatabase = null,
        ),
        gameDatabase = null,
        inventoryManager = inventory,
        preferences = preferences,
        sessionLogger = preferences?.let { SessionLogger(it, GameEventBus()) },
    )

    private fun inventory(
        client: HttpClient,
        vararg counts: Pair<Int, Int>,
    ): InventoryManager = InventoryManager(client, GameEventBus()).also { manager ->
        manager.applyParsedInventory(
            counts.filter { it.second > 0 }.associate { (id, qty) ->
                id to InventoryItem(id, ItemDatabase.getItemName(id).ifEmpty { "item #$id" }, qty, ItemType.OTHER)
            },
        )
    }

    private fun potteryYoYoConcoction() = ConcoctionData(
        result = "pottery yo-yo",
        resultQuantity = 1,
        methods = setOf("MUSE"),
        ingredients = listOf(ConcoctionIngredient("smoked potsherd", 5)),
    )

    private fun brickoBatConcoction() = ConcoctionData(
        result = "BRICKO bat",
        resultQuantity = 1,
        methods = setOf("MUSE", "NOBEE"),
        ingredients = listOf(
            ConcoctionIngredient("BRICKO brick", 5),
            ConcoctionIngredient("BRICKO eye brick", 1),
        ),
    )

    private fun palmFrondFanConcoction() = ConcoctionData(
        result = "palm-frond fan",
        resultQuantity = 1,
        methods = setOf("MUSE"),
        ingredients = listOf(ConcoctionIngredient("palm frond", 2)),
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
        private const val POTSHERD_ID = 88001
        private const val BRICK_ID = 88002
        private const val EYE_BRICK_ID = 88003
        private const val SINGLE_ID = 88004
        private const val PALM_FROND_ID = 2605
        private const val PALM_FROND_FAN_ID = 2606
        private const val PALM_FROND_TOKEN_ID = 88005
    }
}
