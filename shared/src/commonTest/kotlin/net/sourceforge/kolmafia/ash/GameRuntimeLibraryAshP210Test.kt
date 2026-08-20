package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.MysticShopSync
import net.sourceforge.kolmafia.shop.ShoreShopSync

class GameRuntimeLibraryAshP210Test {

    private companion object {
        const val PIXEL_PILL = 5906
        const val RED_PIXEL = 461
        const val WHITE_PIXEL = 459
        const val CHEAP_TOASTER = 637
        const val SHORE_SCRIP = 338
    }

    @AfterTest
    fun tearDown() {
        CoinmasterDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase207() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mystic_validateDeniedUntilVisitHook() {
        registerItem(PIXEL_PILL, "pixel pill")
        registerItem(RED_PIXEL, "red pixel")
        registerItem(WHITE_PIXEL, "white pixel")
        CoinmasterDatabase.loadFromText(
            shopsText = "mystic\tThe Crackpot Mystic's Shed\n",
            coinText = "The Crackpot Mystic's Shed\tROW39\tpixel pill\tred pixel (20)\twhite pixel (20)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(level = "6"))
            },
            inventoryManager = inventoryWithPixels(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item($PIXEL_PILL, true));""").trim(),
        )
        lib.processVisitResponseHooks(
            html = """<tr rel="$PIXEL_PILL"><b>pixel pill</b></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mystic",
        )
        assertTrue(prefs.getBoolean(MysticShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, false))
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item($PIXEL_PILL, true));""").trim(),
        )
    }

    @Test
    fun shore_validateDeniedUntilVisitHook() {
        registerItem(CHEAP_TOASTER, "cheap toaster")
        registerItem(SHORE_SCRIP, "Shore Inc. Ship Trip Scrip")
        CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW637\tcheap toaster\tShore Inc. Ship Trip Scrip (20)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setInt("lastDesertUnlock", 1)
        prefs.setBoolean(ShoreShopSync.CHEAP_TOASTER_BOUGHT_PREF, true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(level = "6", ascensions = "1"))
            },
            inventoryManager = inventoryWithScrip(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item($CHEAP_TOASTER, true));""").trim(),
        )
        lib.processVisitResponseHooks(
            html = """<b>cheap toaster</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
        )
        assertFalse(prefs.getBoolean(ShoreShopSync.CHEAP_TOASTER_BOUGHT_PREF, true))
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item($CHEAP_TOASTER, true));""").trim(),
        )
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun inventoryWithPixels(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        RED_PIXEL to InventoryItem(RED_PIXEL, "red pixel", 20, ItemType.OTHER),
                        WHITE_PIXEL to InventoryItem(WHITE_PIXEL, "white pixel", 20, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }

    private fun inventoryWithScrip(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        SHORE_SCRIP to InventoryItem(SHORE_SCRIP, "Shore Inc. Ship Trip Scrip", 20, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
}
