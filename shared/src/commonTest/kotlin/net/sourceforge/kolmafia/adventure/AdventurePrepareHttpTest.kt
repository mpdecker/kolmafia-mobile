package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.prep.AdventureGateContext
import net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions
import net.sourceforge.kolmafia.adventure.prep.ItemIds
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UseItemRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdventurePrepareHttpTest {

    @AfterTest
    fun tearDown() {
        AdventurePrep.resetForTest()
    }

    @Test
    fun guanoJunction_failsWithoutStenchResistance() = runBlocking {
        val prefs = Preferences(MapSettings())
        val ctx = AdventureGateContext(preferences = prefs)
        val ok = AdventurePrepareActions.prepare(
            locationName = "Guano Junction",
            zone = null,
            ctx = ctx,
            deps = AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
                stenchResistanceLevels = { 0 },
            ),
        )
        assertFalse(ok)
    }

    @Test
    fun guanoJunction_passesWithStenchResistance() = runBlocking {
        val prefs = Preferences(MapSettings())
        val ctx = AdventureGateContext(preferences = prefs)
        val ok = AdventurePrepareActions.prepare(
            locationName = "Guano Junction",
            zone = null,
            ctx = ctx,
            deps = AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
                stenchResistanceLevels = { 1 },
            ),
        )
        assertTrue(ok)
    }

    @Test
    fun rabbitHole_usesDrinkMeWhenEffectMissing() = runBlocking {
        val used = mutableListOf<Int>()
        val prefs = Preferences(MapSettings())
        val ctx = AdventureGateContext(preferences = prefs)
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val useReq = object : UseItemRequest(client) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                used += itemId
                return Result.success("ok")
            }
        }
        AdventurePrepareActions.prepare(
            locationName = "The Rabbit Hole",
            zone = AdventureZone(
                zoneName = "Rabbit Hole",
                urlParams = "adventure.php?snarfblat=1",
                locationName = "The Rabbit Hole",
                environment = "indoor",
                diffLevel = "0",
                statRequirement = 0,
                goals = emptyList(),
                isOverdrunk = false,
                noWander = false,
            ),
            ctx = ctx,
            deps = AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = useReq,
                gameDatabase = null,
                hasEffect = { false },
            ),
        )
        assertTrue(ItemIds.DRINK_ME_POTION in used)
    }
}
