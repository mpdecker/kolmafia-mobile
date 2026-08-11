package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandWarVisitLogSync
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarCampCoinmasterSyncTest {

    @BeforeTest
    fun resetCoinmasterDatabase() {
        CoinmasterDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun sessionLogger(prefs: Preferences): SessionLogger =
        SessionLogger(prefs, GameEventBus())

    private fun sessionLog(prefs: Preferences): String =
        prefs.getString(SessionLogger.SESSION_LOG_KEY, "")

    @Test
    fun dimemasterBuyUrl_containsWhichcamp() = kotlinx.coroutines.runBlocking {
        GameDatabase().load()
        val dimemaster = CoinmasterRegistry.findByNickname("dimemaster")!!
        assertTrue(dimemaster.buyUrl!!.contains("whichcamp=1"))
        assertTrue(dimemaster.sellUrl!!.contains("whichcamp=1"))
    }

    @Test
    fun buy_dimemaster_decrementsDimesAndLogs() = runTest {
        GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setInt("availableDimes", 20)
        val waterPipeBombId = 2348
        val flow = MutableStateFlow(InventoryState())
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
            override val state = flow
            override suspend fun fetchInventory() {
                flow.value = InventoryState(
                    items = mapOf(
                        waterPipeBombId to InventoryItem(waterPipeBombId, "water pipe bomb", 1, ItemType.OTHER),
                    ),
                )
            }
        }
        val client = HttpClient(MockEngine { respond("You've currently got 19 dimes in your pocket.", HttpStatusCode.OK) })
        val manager = CoinmasterManager(
            coinmasterRequest = CoinmasterRequest(client),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            client = client,
            preferences = prefs,
            sessionLogger = logger,
        )
        val dimemaster = CoinmasterRegistry.findByNickname("dimemaster")!!
        assertEquals(1, manager.buy(dimemaster, waterPipeBombId, 1))
        assertEquals(19, prefs.getInt("availableDimes", 0))
        assertTrue(sessionLog(prefs).contains("trading 1 dime for 1 water pipe bomb"))
    }

    @Test
    fun sell_quartersmaster_incrementsQuartersAndLogs() = runTest {
        GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        val hippyButtonId = 2029
        val flow = MutableStateFlow(
            InventoryState(
                items = mapOf(
                    hippyButtonId to InventoryItem(hippyButtonId, "hippy protest button", 1, ItemType.OTHER),
                ),
            ),
        )
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
            override val state = flow
            override suspend fun fetchInventory() {
                flow.value = InventoryState()
            }
        }
        val client = HttpClient(MockEngine { respond("You've currently got 1 quarter in your pocket.", HttpStatusCode.OK) })
        val manager = CoinmasterManager(
            coinmasterRequest = CoinmasterRequest(client),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            client = client,
            preferences = prefs,
            sessionLogger = logger,
        )
        val quartersmaster = CoinmasterRegistry.findByNickname("quartersmaster")!!
        assertEquals(1, manager.sell(quartersmaster, hippyButtonId, 1))
        assertEquals(1, prefs.getInt("availableQuarters", 0))
        val log = sessionLog(prefs)
        assertTrue(log.contains("trading 1 hippy protest button for 1 quarter"))
        assertTrue(log.contains("You acquire 1 quarter"))
    }

    @Test
    fun buy_insufficient_noPrefChange() = runTest {
        GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setInt("availableDimes", 20)
        val waterPipeBombId = 2348
        val flow = MutableStateFlow(InventoryState())
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
            override val state = flow
            override suspend fun fetchInventory() {}
        }
        val client = HttpClient(MockEngine { respond("You don't have enough dimes for that.", HttpStatusCode.OK) })
        val manager = CoinmasterManager(
            coinmasterRequest = CoinmasterRequest(client),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            client = client,
            preferences = prefs,
            sessionLogger = logger,
        )
        val dimemaster = CoinmasterRegistry.findByNickname("dimemaster")!!
        assertEquals(0, manager.buy(dimemaster, waterPipeBombId, 1))
        assertEquals(20, prefs.getInt("availableDimes", 0))
        assertFalse(sessionLog(prefs).contains("trading"))
    }

    @Test
    fun buy_setsLastCampVisited() = runTest {
        GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setInt("availableDimes", 20)
        val waterPipeBombId = 2348
        val flow = MutableStateFlow(InventoryState())
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
            override val state = flow
            override suspend fun fetchInventory() {
                flow.value = InventoryState(
                    items = mapOf(
                        waterPipeBombId to InventoryItem(waterPipeBombId, "water pipe bomb", 1, ItemType.OTHER),
                    ),
                )
            }
        }
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val manager = CoinmasterManager(
            coinmasterRequest = CoinmasterRequest(client),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            client = client,
            preferences = prefs,
            sessionLogger = logger,
        )
        val dimemaster = CoinmasterRegistry.findByNickname("dimemaster")!!
        manager.buy(dimemaster, waterPipeBombId, 1)
        assertEquals("dimemaster", prefs.getString(IslandWarVisitLogSync.PREF_LAST_CAMP_VISITED, ""))
    }
}
