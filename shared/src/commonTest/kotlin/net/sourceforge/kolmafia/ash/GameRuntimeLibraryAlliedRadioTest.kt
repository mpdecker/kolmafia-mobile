package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AlliedRadioRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.AlliedRadioManager
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.session.DemonInCombatNameSync

class GameRuntimeLibraryAlliedRadioTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private class TestInventoryManager(
        client: HttpClient,
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(client, GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    @Test
    fun alliedradio_rejectsWithoutRadio() {
        val out = outputLib(
            GameRuntimeLibrary(
                preferences = prefs(),
                alliedRadioManager = AlliedRadioManager(
                    prefs(),
                    AlliedRadioRequest(HttpClient(MockEngine { respond("ok") }), UseItemRequest(HttpClient(MockEngine { respond("ok") }))),
                    null,
                    null,
                ),
            ),
            """cli_execute("alliedradio item fuel");""",
        )
        assertTrue(out.contains("handheld radio"))
    }

    @Test
    fun alliedradio_dispatchesWithBackpack() {
        val prefs = prefs()
        val client = HttpClient(MockEngine { respond("Thanks.") })
        var requestText = ""
        val inv = TestInventoryManager(
            client,
            mapOf(
                BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID to InventoryItem(
                    BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID,
                    "Allied Radio Backpack",
                    1,
                    ItemType.OTHER,
                ),
            ),
        )
        val mgr = AlliedRadioManager(
            preferences = prefs,
            request = object : AlliedRadioRequest(client, UseItemRequest(client)) {
                override suspend fun requestRadioCall(text: String, handheld: Boolean): Result<RadioResult> {
                    requestText = text
                    return Result.success(RadioResult("Thanks.", false))
                }
            },
            inventoryManager = inv,
            segmentSync = DemonInCombatNameSync(prefs),
        )
        val character = KoLCharacter().apply {
            updateEquipment(EquipmentSlot.CONTAINER, "Allied Radio Backpack")
        }
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            inventoryManager = inv,
            character = character,
            alliedRadioManager = mgr,
        )
        outputLib(lib, """cli_execute("alliedradio effect ell");""")
        assertTrue(requestText == "ellipsoidtine")
    }
}
