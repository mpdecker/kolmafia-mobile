package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.SummoningChamberRequest

class SummoningChamberManagerTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun resolveDemon_byNumber13_afterYegSync() {
        val p = prefs()
        CultShortsDatabase.injectForTest(listOf(373, 222, 7, 602, 172, 251, 282))
        val sync = YegDemonNameSync(p)
        sync.updateYegName(
            mapOf(
                373 to "Yeg",
                222 to "the",
                7 to "Eld",
                602 to "ritch",
                172 to "One",
                251 to "True",
                282 to "Name",
            ),
        )
        val mgr = SummoningChamberManager(p, SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })), null, null, null, null)
        val resolved = mgr.resolveDemon("13")
        assertEquals(13, resolved?.number)
        assertEquals("YegtheEldritchOneTrueName", resolved?.name)
        CultShortsDatabase.resetForTest()
    }

    @Test
    fun resolveDemon_byNumber() {
        val p = prefs()
        p.setString("demonName7", "Ak'gyxoth")
        val mgr = SummoningChamberManager(p, SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })), null, null, null, null)
        val resolved = mgr.resolveDemon("7")
        assertEquals(7, resolved?.number)
        assertEquals("Ak'gyxoth", resolved?.name)
    }

    @Test
    fun resolveDemon_byLocation() {
        val p = prefs()
        p.setString("demonName1", "Pie Lord")
        val mgr = SummoningChamberManager(p, SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })), null, null, null, null)
        val resolved = mgr.resolveDemon("Summoning Chamber")
        assertEquals(1, resolved?.number)
        assertEquals("Pie Lord", resolved?.name)
    }

    @Test
    fun resolveDemon_byEffect() {
        val p = prefs()
        p.setString("demonName2", "Greed Demon")
        val mgr = SummoningChamberManager(p, SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })), null, null, null, null)
        val resolved = mgr.resolveDemon("Preternatural Greed")
        assertEquals(2, resolved?.number)
        assertEquals("Greed Demon", resolved?.name)
    }

    @Test
    fun summon_rejectsWhenAlreadySummoned() = runTest {
        val p = prefs()
        p.setBoolean(Preferences.DEMON_SUMMONED, true)
        p.setString("demonName7", "Ak'gyxoth")
        val messages = mutableListOf<String>()
        val mgr = SummoningChamberManager(
            p,
            SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })),
            null,
            null,
            null,
            null,
        )
        val result = mgr.summon("7") { messages += it }
        assertTrue(result.isFailure)
        assertEquals(listOf("You've already summoned a demon today."), messages)
    }

    @Test
    fun summon_demon12RequiresNeilPrefix() = runTest {
        val p = prefs()
        p.setString("demonName12", "Ak'gyxoth")
        val messages = mutableListOf<String>()
        val retrieve = object : RetrieveItemService(null, null, null, null, null, null, null, null, null, null, null) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int = qty
        }
        val mgr = SummoningChamberManager(
            p,
            SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })),
            retrieve,
            null,
            null,
            null,
        )
        val result = mgr.summon("12") { messages += it }
        assertTrue(result.isFailure)
        assertEquals(listOf("You don't know the full name of that demon."), messages)
    }

    @Test
    fun summon_successConsumesItemsAndSetsFlag() = runTest {
        val p = prefs()
        p.setString("demonName7", "Ak'gyxoth")
        val retrieve = object : RetrieveItemService(null, null, null, null, null, null, null, null, null, null, null) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int = qty
        }
        var candlesConsumed = 0
        var scrollsConsumed = 0
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                when (itemId) {
                    DemonTypes.BLACK_CANDLE -> candlesConsumed += quantity
                    DemonTypes.EVIL_SCROLL -> scrollsConsumed += quantity
                }
            }
        }
        val recording = object : SummoningChamberRequest(HttpClient(MockEngine { respond("ok") })) {
            var lastDemonName: String? = null
            override suspend fun summon(demonName: String): Result<SummonResult> {
                lastDemonName = demonName
                return Result.success(
                    SummonResult(
                        SummoningChamberRequest.buildLocation(demonName),
                        "You light three black candles.",
                    ),
                )
            }
        }
        val mgr = SummoningChamberManager(p, recording, retrieve, inv, null, null)
        val messages = mutableListOf<String>()
        val result = mgr.summon("7") { messages += it }
        assertTrue(result.isSuccess)
        assertEquals("Ak'gyxoth", recording.lastDemonName)
        assertTrue(p.getBoolean(Preferences.DEMON_SUMMONED, false))
        assertEquals(3, candlesConsumed)
        assertEquals(1, scrollsConsumed)
    }
}
