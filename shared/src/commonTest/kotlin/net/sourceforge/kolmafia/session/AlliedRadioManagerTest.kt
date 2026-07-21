package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AlliedRadioRequest
import net.sourceforge.kolmafia.request.UseItemRequest

class AlliedRadioManagerTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun invWith(vararg items: Pair<Int, Int>): InventoryState =
        InventoryState(
            items = items.associate { (id, qty) ->
                id to InventoryItem(id, "item$id", qty, ItemType.OTHER)
            },
        )

    private fun charWithBackpackEquipped(): CharacterState =
        CharacterState(
            equipment = mapOf(EquipmentSlot.CONTAINER to "Allied Radio Backpack"),
        )

    @Test
    fun usesRemaining_countsHandheldAndBackpack() {
        val mgr = AlliedRadioManager(prefs(), AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))), null, null)
        val uses = mgr.usesRemaining(
            invWith(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID to 2),
            charWithBackpackEquipped(),
        )
        assertEquals(5, uses)
    }

    @Test
    fun lacksRadioAndBackpack_whenNothingAvailable() {
        val mgr = AlliedRadioManager(prefs(), AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))), null, null)
        assertTrue(mgr.lacksRadioAndBackpack(InventoryState(), null))
    }

    @Test
    fun resolveEffectRequest_mapsEllipsoidtine() {
        val mgr = AlliedRadioManager(prefs(), AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))), null, null)
        val messages = mutableListOf<String>()
        assertEquals("ellipsoidtine", mgr.resolveEffectRequest("ell", messages::add))
    }

    @Test
    fun resolveItemRequest_mapsFuelAlias() {
        val mgr = AlliedRadioManager(prefs(), AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))), null, null)
        val messages = mutableListOf<String>()
        assertEquals("fuel", mgr.resolveItemRequest("booze", messages::add))
    }

    @Test
    fun resolveMiscRequest_mapsSniperSupport() {
        val mgr = AlliedRadioManager(prefs(), AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))), null, null)
        val messages = mutableListOf<String>()
        assertEquals("sniper support", mgr.resolveMiscRequest("support", messages::add))
    }

    @Test
    fun run_rejectsWhenNoRadio() = runTest {
        val mgr = AlliedRadioManager(
            prefs(),
            AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))),
            null,
            null,
        )
        val messages = mutableListOf<String>()
        val result = mgr.run("item fuel", InventoryState(), null, messages::add)
        assertFalse(result.isSuccess)
        assertTrue(messages.any { it.contains("handheld radio") })
    }

    @Test
    fun submitRequest_usesBackpackWhenAvailable() = runTest {
        val engine = MockEngine {
            respond("Thanks.", HttpStatusCode.OK)
        }
        val client = HttpClient(engine)
        val prefs = prefs()
        val segmentSync = DemonInCombatNameSync(prefs)
        val invMgr = InventoryManager(client, GameEventBus())
        val request = object : AlliedRadioRequest(client, UseItemRequest(client)) {
            var openBackpackCalls = 0
            var handheld = false
            override suspend fun openBackpack(): Result<Unit> {
                openBackpackCalls++
                return Result.success(Unit)
            }
            override suspend fun useHandheldRadio(): Result<String> {
                handheld = true
                return Result.success("ok")
            }
            override suspend fun submitRequest(request: String, handheld: Boolean) =
                Result.success(RadioResult("Thanks.", handheld))
        }
        val mgr = AlliedRadioManager(prefs, request, invMgr, segmentSync)
        val messages = mutableListOf<String>()
        val result = mgr.submitRequest(
            "fuel",
            invWith(BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID to 1),
            charWithBackpackEquipped(),
            messages::add,
        )
        assertTrue(result.isSuccess)
        assertEquals(1, request.openBackpackCalls)
        assertFalse(request.handheld)
        assertEquals(1, prefs.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0))
    }

    @Test
    fun submitRequest_consumesHandheldWhenBackpackExhausted() = runTest {
        val client = HttpClient(MockEngine { respond("Thanks.", HttpStatusCode.OK) })
        val prefs = prefs()
        prefs.setInt(Preferences.ALLIED_RADIO_DROPS_USED, 3)
        var consumed = false
        val invMgr = object : InventoryManager(client, GameEventBus()) {
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                consumed = true
                assertEquals(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID, itemId)
            }
        }
        val request = object : AlliedRadioRequest(client, UseItemRequest(client)) {
            override suspend fun requestRadioCall(requestText: String, handheld: Boolean) =
                Result.success(RadioResult("Thanks.", true))
        }
        val mgr = AlliedRadioManager(prefs, request, invMgr, null)
        val result = mgr.submitRequest(
            "fuel",
            invWith(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID to 1),
            charWithBackpackEquipped(),
        ) {}
        assertTrue(result.isSuccess)
        assertTrue(consumed)
    }
}
