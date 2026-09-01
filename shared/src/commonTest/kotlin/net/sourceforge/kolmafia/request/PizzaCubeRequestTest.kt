package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
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
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class PizzaCubeRequestTest {

    @Test
    fun makePizza_postsCampgroundActionPizzaWithFourIds() = runTest {
        registerPizzaItems()
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = recordingClient(requests)
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)
        val request = PizzaCubeRequest(client, inventory, preferences, sessionLogger(preferences))

        val result = request.makePizza(listOf(ING_A, ING_B, ING_C, ING_D))

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf("/campground.php"), requests.map { it.second })
        assertEquals(listOf(HttpMethod.Post), requests.map { it.first })
        assertForm(requests.single().third, "action", "pizza")
        assertForm(requests.single().third, "pizza", "$ING_A,$ING_B,$ING_C,$ING_D")
        assertEquals(0, inventory.getCount(ING_A))
        assertEquals(0, inventory.getCount(ING_B))
        assertEquals(0, inventory.getCount(ING_C))
        assertEquals(0, inventory.getCount(ING_D))
        assertEquals(1, inventory.getCount(DIABOLIC_PIZZA_ID))
        assertEquals("$ING_A,$ING_B,$ING_C,$ING_D", preferences.getString("lastDiabolicPizza", ""))
        assertTrue(preferences.getString(SessionLogger.SESSION_LOG_KEY, "").contains("pizza "))
    }

    @Test
    fun makePizza_parseResponseMakepizzaConsumesIngredientsWithoutInventingGain() = runTest {
        registerPizzaItems()
        val client = HttpClient(MockEngine { respond(UNKNOWN_GAIN_HTML, HttpStatusCode.OK) })
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)
        val request = PizzaCubeRequest(client, inventory, preferences, null)
        val url = "campground.php?action=makepizza&pizza=$ING_A,$ING_B,$ING_C,$ING_D"

        val parsed = request.parseResponse(url, UNKNOWN_GAIN_HTML)

        assertTrue(parsed)
        assertEquals(0, inventory.getCount(ING_A))
        assertEquals(0, inventory.getCount(ING_B))
        assertEquals(0, inventory.getCount(ING_C))
        assertEquals(0, inventory.getCount(ING_D))
        assertEquals(0, inventory.getCount(DIABOLIC_PIZZA_ID))
        assertEquals("$ING_A,$ING_B,$ING_C,$ING_D", preferences.getString("lastDiabolicPizza", ""))
    }

    @Test
    fun makePizza_malformedResponsePreservesInventoryAndPrefs() = runTest {
        registerPizzaItems()
        val client = HttpClient(MockEngine {
            respond("<html>the cube sits there, unimpressed</html>", HttpStatusCode.OK)
        })
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)
        val request = PizzaCubeRequest(client, inventory, preferences, sessionLogger(preferences))

        val result = request.makePizza(listOf(ING_A, ING_B, ING_C, ING_D))

        assertTrue(result.isFailure)
        assertEquals(1, inventory.getCount(ING_A))
        assertEquals(1, inventory.getCount(ING_B))
        assertEquals(1, inventory.getCount(ING_C))
        assertEquals(1, inventory.getCount(ING_D))
        assertEquals(0, inventory.getCount(DIABOLIC_PIZZA_ID))
        assertEquals("", preferences.getString("lastDiabolicPizza", ""))
        assertEquals("", preferences.getString(SessionLogger.SESSION_LOG_KEY, ""))
    }

    @Test
    fun makePizza_failedHttpPreservesInventory() = runTest {
        registerPizzaItems()
        val client = HttpClient(MockEngine { respond("no", HttpStatusCode.InternalServerError) })
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)
        val request = PizzaCubeRequest(client, inventory, preferences, null)

        val result = request.makePizza(listOf(ING_A, ING_B, ING_C, ING_D))

        assertTrue(result.isFailure)
        assertEquals(1, inventory.getCount(ING_A))
        assertEquals("", preferences.getString("lastDiabolicPizza", ""))
    }

    @Test
    fun makePizza_rejectsInvalidOrUnownedIngredientsWithoutHttp() = runTest {
        registerPizzaItems()
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(SUCCESS_HTML, HttpStatusCode.OK)
        })
        val preferences = pizzaPrefs()
        val missing = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1)
        val owned = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)

        assertTrue(
            PizzaCubeRequest(client, missing, preferences, null)
                .makePizza(listOf(ING_A, ING_B, ING_C, ING_D))
                .isFailure,
        )
        assertTrue(
            PizzaCubeRequest(client, owned, preferences, null)
                .makePizza(listOf(ING_A, ING_B, ING_C, 0))
                .isFailure,
        )
        assertTrue(
            PizzaCubeRequest(client, owned, preferences, null)
                .makePizza(listOf(ING_A, ING_B, ING_C))
                .isFailure,
        )
        assertEquals(0, calls)
        assertEquals(1, missing.getCount(ING_A))
    }

    @Test
    fun makePizza_duplicateIngredientsAllowedWhenOwned() = runTest {
        registerPizzaItems()
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = recordingClient(requests)
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 3, ING_B to 1)
        val request = PizzaCubeRequest(client, inventory, preferences, null)

        val result = request.makePizza(listOf(ING_A, ING_A, ING_B, ING_A))

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertForm(requests.single().third, "pizza", "$ING_A,$ING_A,$ING_B,$ING_A")
        assertEquals(0, inventory.getCount(ING_A))
        assertEquals(0, inventory.getCount(ING_B))
        assertEquals(1, inventory.getCount(DIABOLIC_PIZZA_ID))
    }

    @Test
    fun makePizza_twoIdenticalCallsEachConsumeAndGain() = runTest {
        registerPizzaItems()
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = recordingClient(requests)
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 2, ING_B to 2, ING_C to 2, ING_D to 2)
        val request = PizzaCubeRequest(client, inventory, preferences, sessionLogger(preferences))
        val ingredients = listOf(ING_A, ING_B, ING_C, ING_D)

        val first = request.makePizza(ingredients)
        val second = request.makePizza(ingredients)

        assertTrue(first.isSuccess, first.exceptionOrNull()?.message)
        assertTrue(second.isSuccess, second.exceptionOrNull()?.message)
        assertEquals(2, requests.size)
        assertEquals(0, inventory.getCount(ING_A))
        assertEquals(0, inventory.getCount(ING_B))
        assertEquals(0, inventory.getCount(ING_C))
        assertEquals(0, inventory.getCount(ING_D))
        assertEquals(2, inventory.getCount(DIABOLIC_PIZZA_ID))
        assertEquals(2, preferences.getString(SessionLogger.SESSION_LOG_KEY, "").split("pizza ").size - 1)
    }

    @Test
    fun makePizza_abortsWithoutHttpWhenInFightOrChoice() = runTest {
        registerPizzaItems()
        RequestAbortGate.resetForTest()
        try {
            RequestAbortGate.inFightProvider = { true }
            var calls = 0
            val client = HttpClient(MockEngine {
                calls++
                respond(SUCCESS_HTML, HttpStatusCode.OK)
            })
            val preferences = pizzaPrefs()
            val inventory = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)
            val request = PizzaCubeRequest(client, inventory, preferences, null)

            val result = request.makePizza(listOf(ING_A, ING_B, ING_C, ING_D))

            assertTrue(result.isFailure)
            assertEquals(0, calls)
            assertEquals(1, inventory.getCount(ING_A))
            assertEquals(0, inventory.getCount(DIABOLIC_PIZZA_ID))
            assertEquals("", preferences.getString("lastDiabolicPizza", ""))
        } finally {
            RequestAbortGate.resetForTest()
        }
    }

    @Test
    fun makePizza_thenVisitHook_doesNotDoubleGain() = runTest {
        registerPizzaItems()
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = recordingClient(requests)
        val preferences = pizzaPrefs()
        val inventory = inventory(client, ING_A to 1, ING_B to 1, ING_C to 1, ING_D to 1)
        val request = PizzaCubeRequest(client, inventory, preferences, sessionLogger(preferences))
        val library = GameRuntimeLibrary(
            preferences = preferences,
            inventoryManager = inventory,
            pizzaCubeRequest = request,
        )
        val ingredients = listOf(ING_A, ING_B, ING_C, ING_D)

        val result = request.makePizza(ingredients)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        library.processVisitResponseHooks(
            result.getOrThrow(),
            "campground.php?action=pizza&pizza=$ING_A,$ING_B,$ING_C,$ING_D",
        )

        assertEquals(1, requests.size)
        assertEquals(0, inventory.getCount(ING_A), "ingredients=${inventory.getCount(ING_A)}")
        assertEquals(1, inventory.getCount(DIABOLIC_PIZZA_ID), "pizza=${inventory.getCount(DIABOLIC_PIZZA_ID)}")
        assertEquals("$ING_A,$ING_B,$ING_C,$ING_D", preferences.getString("lastDiabolicPizza", ""))
    }

    @Test
    fun visitHook_routesCampgroundPizzaIdempotently() {
        registerPizzaItems()
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        preferences.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, DIABOLIC_PIZZA_CUBE_ID)
        val inventory = inventory(
            HttpClient(MockEngine { respond("") }),
            ING_A to 1,
            ING_B to 1,
            ING_C to 1,
            ING_D to 1,
        )
        val library = GameRuntimeLibrary(
            preferences = preferences,
            inventoryManager = inventory,
        )
        val url = "campground.php?action=makepizza&pizza=$ING_A,$ING_B,$ING_C,$ING_D"

        library.processVisitResponseHooks(SUCCESS_HTML, url)
        library.processVisitResponseHooks(SUCCESS_HTML, url)

        assertEquals(0, inventory.getCount(ING_A))
        assertEquals(0, inventory.getCount(ING_B))
        assertEquals(0, inventory.getCount(ING_C))
        assertEquals(0, inventory.getCount(ING_D))
        assertEquals(1, inventory.getCount(DIABOLIC_PIZZA_ID), "visit hook should processResults once")
        assertEquals(1, settings.pizzaWrites)
        assertEquals("$ING_A,$ING_B,$ING_C,$ING_D", preferences.getString("lastDiabolicPizza", ""))
    }

    @Test
    fun visitHook_malformedPizzaResponseDoesNotConsume() {
        registerPizzaItems()
        val preferences = pizzaPrefs()
        val inventory = inventory(
            HttpClient(MockEngine { respond("") }),
            ING_A to 1,
            ING_B to 1,
            ING_C to 1,
            ING_D to 1,
        )
        val library = GameRuntimeLibrary(
            preferences = preferences,
            inventoryManager = inventory,
        )

        library.processVisitResponseHooks(
            "<html>the cube sits there, unimpressed</html>",
            "campground.php?action=pizza&pizza=$ING_A,$ING_B,$ING_C,$ING_D",
        )

        assertEquals(1, inventory.getCount(ING_A))
        assertEquals(0, inventory.getCount(DIABOLIC_PIZZA_ID))
        assertEquals("", preferences.getString("lastDiabolicPizza", ""))
    }

    private fun recordingClient(
        requests: MutableList<Triple<HttpMethod, String, String>>,
    ): HttpClient = HttpClient(MockEngine { request ->
        requests += Triple(request.method, request.url.encodedPath, request.body.toByteArray().decodeToString())
        respond(SUCCESS_HTML, HttpStatusCode.OK)
    })

    private fun pizzaPrefs(): Preferences = Preferences(MapSettings()).apply {
        setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, DIABOLIC_PIZZA_CUBE_ID)
    }

    private fun sessionLogger(preferences: Preferences) = SessionLogger(preferences, GameEventBus())

    private fun inventory(
        client: HttpClient,
        vararg counts: Pair<Int, Int>,
    ): InventoryManager = InventoryManager(client, GameEventBus()).also { manager ->
        manager.applyParsedInventory(
            buildMap {
                counts.filter { it.second > 0 }.forEach { (id, qty) ->
                    put(id, InventoryItem(id, ItemDatabase.getItemName(id).ifEmpty { "item #$id" }, qty, ItemType.OTHER))
                }
            },
        )
    }

    private fun registerPizzaItems() {
        registerItem(ING_A, "test dough")
        registerItem(ING_B, "test sauce")
        registerItem(ING_C, "test cheese")
        registerItem(ING_D, "test topping")
        registerItem(DIABOLIC_PIZZA_ID, "diabolic pizza")
        registerItem(DIABOLIC_PIZZA_CUBE_ID, "diabolic pizza cube")
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
                autosellPrice = 5,
                plural = null,
            ),
        )
    }

    private fun assertForm(body: String, key: String, expected: String) {
        val actual = Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)
            ?.replace("%2C", ",", ignoreCase = true)
        assertEquals(expected, actual, body)
    }

    private class CountingSettings(
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        var pizzaWrites: Int = 0
            private set

        override fun putString(key: String, value: String) {
            if (key == "lastDiabolicPizza") pizzaWrites++
            delegate.putString(key, value)
        }
    }

    companion object {
        private const val ING_A = 88101
        private const val ING_B = 88102
        private const val ING_C = 88103
        private const val ING_D = 88104
        private const val DIABOLIC_PIZZA_ID = 10336
        private const val DIABOLIC_PIZZA_CUBE_ID = 10335
        private const val SUCCESS_HTML = "You acquire an item: <b>diabolic pizza</b>"
        private const val UNKNOWN_GAIN_HTML = "You acquire an item: <b>mystery pie</b>"
    }
}
