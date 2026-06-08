package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreakfastManagerTest {

    private val mockClient = HttpClient(MockEngine { respond("ok") })

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun charState(
        isHardcore: Boolean = false,
        classId: Int = 1, // Seal Clubber = 1 (muscle class)
        hasClan: Boolean = true,
    ) = CharacterState(
        isHardcore = isHardcore,
        characterClass = classId,
        hasClan = hasClan,
    )

    private fun inventoryWithItems(vararg ids: Int): InventoryState {
        val items = ids.associate { id ->
            id to InventoryItem(itemId = id, name = "Item $id", quantity = 1, type = ItemType.OTHER)
        }
        return InventoryState(items = items)
    }

    private fun manager(
        prefs: Preferences = prefs(),
        gardenCalls: MutableList<Unit> = mutableListOf(),
        rumbusCalls: MutableList<Unit> = mutableListOf(),
        klawCalls: MutableList<Unit> = mutableListOf(),
        useItemRequest: UseItemRequest = UseItemRequest(mockClient),
    ): BreakfastManager {
        val campground = object : CampgroundRequest(mockClient) {
            override suspend fun harvestGarden() = Result.success(Unit).also { gardenCalls.add(Unit) }
        }
        val rumpus = object : ClanRumpusRequest(mockClient) {
            override suspend fun visit() = Result.success(Unit).also { rumbusCalls.add(Unit) }
        }
        val lounge = object : ClanLoungeRequest(mockClient) {
            override suspend fun useKlaw() = Result.success("ok").also { klawCalls.add(Unit) }
        }
        return BreakfastManager(
            campgroundRequest = campground,
            clanRumpusRequest = rumpus,
            clanLoungeRequest = lounge,
            preferences = prefs,
            useItemRequest = useItemRequest,
        )
    }

    @Test fun runBreakfast_alreadyCompleted_doesNothing() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putBoolean(Preferences.BREAKFAST_COMPLETED, true) }
        manager(prefs = p, gardenCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty())
    }

    @Test fun runBreakfast_gardenSkippedWhenPrefNone_softcore() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putString(Preferences.HARVEST_GARDEN_SOFTCORE, "none") }
        manager(prefs = p, gardenCalls = calls).runBreakfast(charState(isHardcore = false), InventoryState())
        assertTrue(calls.isEmpty(), "garden should not be called when pref=none")
    }

    @Test fun runBreakfast_gardenSkippedWhenSentinelSet() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs {
            putString(Preferences.HARVEST_GARDEN_SOFTCORE, "any")
            putBoolean(Preferences.GARDEN_HARVESTED, true)
        }
        manager(prefs = p, gardenCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty())
    }

    @Test fun runBreakfast_gardenCalled_andSentinelSet() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putString(Preferences.HARVEST_GARDEN_SOFTCORE, "any") }  // explicitly opt in
        val mgr = manager(prefs = p, gardenCalls = calls)
        mgr.runBreakfast(charState(), InventoryState())
        assertEquals(1, calls.size)
        assertTrue(p.getBoolean(Preferences.GARDEN_HARVESTED))
    }

    @Test fun runBreakfast_gardenNetworkFailure_continuesAndDoesNotSetSentinel() = runBlocking {
        var gardenCalled = false
        var rumbusCalled = false
        val p = prefs { putString(Preferences.HARVEST_GARDEN_SOFTCORE, "any") }
        val campground = object : CampgroundRequest(mockClient) {
            override suspend fun harvestGarden(): Result<Unit> { gardenCalled = true; return Result.failure(Exception("net")) }
        }
        val rumpus = object : ClanRumpusRequest(mockClient) {
            override suspend fun visit(): Result<Unit> { rumbusCalled = true; return Result.success(Unit) }
        }
        val lounge = object : ClanLoungeRequest(mockClient) {
            override suspend fun useKlaw() = Result.success("ok")
        }
        BreakfastManager(campground, rumpus, lounge, p, UseItemRequest(mockClient)).runBreakfast(charState(), InventoryState())
        assertTrue(gardenCalled)
        assertFalse(p.getBoolean(Preferences.GARDEN_HARVESTED), "sentinel must NOT be set on failure")
        assertTrue(rumbusCalled, "rumpus must still run after garden failure")
    }

    @Test fun runBreakfast_rumpusSkippedWhenPrefFalse() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putBoolean(Preferences.VISIT_RUMPUS_SOFTCORE, false) }
        manager(prefs = p, rumbusCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty())
    }

    @Test fun runBreakfast_rumpusCalled_andSentinelSet() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        manager(prefs = p, rumbusCalls = calls).runBreakfast(charState(), InventoryState())
        assertEquals(1, calls.size)
        assertTrue(p.getBoolean(Preferences.BREAKFAST_RUMPUS))
    }

    @Test fun runBreakfast_vipLoungeSkippedWithoutKey() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        // no VIP key in inventory
        manager(prefs = p, klawCalls = calls).runBreakfast(charState(), InventoryState())
        assertTrue(calls.isEmpty(), "klaw should not fire without VIP key")
    }

    @Test fun runBreakfast_klawLoopsUntilThree() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs()
        val inv = inventoryWithItems(5479) // VIP key
        manager(prefs = p, klawCalls = calls).runBreakfast(charState(), inv)
        assertEquals(3, calls.size)
        assertEquals(3, p.getInt(Preferences.DELUXE_KLAW_SUMMONS))
    }

    @Test fun runBreakfast_klawResumesFromPartialCount() = runBlocking {
        val calls = mutableListOf<Unit>()
        val p = prefs { putInt(Preferences.DELUXE_KLAW_SUMMONS, 2) }
        val inv = inventoryWithItems(5479)
        manager(prefs = p, klawCalls = calls).runBreakfast(charState(), inv)
        assertEquals(1, calls.size, "only 1 more klaw needed")
        assertEquals(3, p.getInt(Preferences.DELUXE_KLAW_SUMMONS))
    }

    @Test fun runBreakfast_setsBreakfastCompletedAtEnd() = runBlocking {
        val p = prefs()
        manager(prefs = p).runBreakfast(charState(), InventoryState())
        assertTrue(p.getBoolean(Preferences.BREAKFAST_COMPLETED))
    }

    @Test fun readGuildManual_callsUseItemRequest_whenManualInInventory() = runBlocking {
        val useItemCalls = mutableListOf<Pair<Int, Int>>()
        val fakeUseItemRequest = object : UseItemRequest(mockClient) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                useItemCalls.add(itemId to quantity)
                return Result.success("ok")
            }
        }
        val p = prefs()
        // classId=1 = Seal Clubber (muscle), so manualId = MUS_MANUAL_ID = 11
        val inv = inventoryWithItems(BreakfastManager.MUS_MANUAL_ID)
        manager(prefs = p, useItemRequest = fakeUseItemRequest).runBreakfast(charState(classId = 1), inv)
        assertEquals(1, useItemCalls.size, "use() should be called once")
        assertEquals(BreakfastManager.MUS_MANUAL_ID to 1, useItemCalls[0])
    }

    @Test fun readGuildManual_setsGuildManualUsed_onSuccess() = runBlocking {
        val fakeUseItemRequest = object : UseItemRequest(mockClient) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> = Result.success("ok")
        }
        val p = prefs()
        val inv = inventoryWithItems(BreakfastManager.MUS_MANUAL_ID)
        manager(prefs = p, useItemRequest = fakeUseItemRequest).runBreakfast(charState(classId = 1), inv)
        assertTrue(p.getBoolean(Preferences.GUILD_MANUAL_USED), "GUILD_MANUAL_USED should be set to true after success")
    }

    @Test fun readGuildManual_skipsWhenGuildManualUsedTrue() = runBlocking {
        val useItemCalls = mutableListOf<Pair<Int, Int>>()
        val fakeUseItemRequest = object : UseItemRequest(mockClient) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                useItemCalls.add(itemId to quantity)
                return Result.success("ok")
            }
        }
        val p = prefs { putBoolean(Preferences.GUILD_MANUAL_USED, true) }
        val inv = inventoryWithItems(BreakfastManager.MUS_MANUAL_ID)
        manager(prefs = p, useItemRequest = fakeUseItemRequest).runBreakfast(charState(classId = 1), inv)
        assertTrue(useItemCalls.isEmpty(), "use() should NOT be called when GUILD_MANUAL_USED is already true")
    }

    @Test fun clearBreakfastPrefs_resetsAllSentinels() {
        val p = prefs {
            putBoolean(Preferences.BREAKFAST_COMPLETED, true)
            putBoolean(Preferences.GARDEN_HARVESTED, true)
            putBoolean(Preferences.BREAKFAST_RUMPUS, true)
            putBoolean(Preferences.GUILD_MANUAL_USED, true)
            putInt(Preferences.DELUXE_KLAW_SUMMONS, 3)
            putBoolean(Preferences.LOOKING_GLASS, true)
            putBoolean(Preferences.FIREWORKS_SHOP, true)
            putInt(Preferences.POOL_GAME_RESULT, 1)
        }
        manager(prefs = p).clearBreakfastPrefs()
        assertFalse(p.getBoolean(Preferences.BREAKFAST_COMPLETED))
        assertFalse(p.getBoolean(Preferences.GARDEN_HARVESTED))
        assertFalse(p.getBoolean(Preferences.BREAKFAST_RUMPUS))
        assertFalse(p.getBoolean(Preferences.GUILD_MANUAL_USED))
        assertEquals(0, p.getInt(Preferences.DELUXE_KLAW_SUMMONS))
        assertFalse(p.getBoolean(Preferences.LOOKING_GLASS))
        assertFalse(p.getBoolean(Preferences.FIREWORKS_SHOP))
        assertEquals(0, p.getInt(Preferences.POOL_GAME_RESULT))
    }

    @Test fun breakfastItemIds_toysMapIsNonEmpty() {
        assertTrue(BreakfastItemIds.TOYS.isNotEmpty(), "TOYS map must not be empty")
        assertEquals(34, BreakfastItemIds.TOYS.size, "Expected 34 toys")
    }

    @Test fun breakfastItemIds_knownItemIdsAreCorrect() {
        assertEquals(24, BreakfastItemIds.CLOVER_ITEM_ID)
        assertEquals(9529, BreakfastItemIds.GENIE_BOTTLE_ID)
        assertEquals(10917, BreakfastItemIds.BOOK_OF_EVERY_SKILL_ID)
        assertEquals(142, BreakfastItemIds.ANTICHEESE_ID)
    }
}
