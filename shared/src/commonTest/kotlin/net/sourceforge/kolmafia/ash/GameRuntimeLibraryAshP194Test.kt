package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ShopRowDatabase

class GameRuntimeLibraryAshP194Test {

    @Test
    fun revision_phase200() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun visitOverlayAuthority_blocksBundledItemNotInVisitInventory() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "mystic\tThe Crackpot Mystic's Shed\tCOIN\n",
            coinText = """
                The Crackpot Mystic's Shed\tROW100\tlisted item A\tred pixel (10)
                The Crackpot Mystic's Shed\tROW101\tlisted item B\twhite pixel (10)
            """.trimIndent(),
        )
        CoinmasterVisitInventory.registerVisitBuyRows(
            "mystic",
            listOf(
                net.sourceforge.kolmafia.shop.ShopRow(
                    rowId = 100,
                    item = net.sourceforge.kolmafia.shop.ItemStack(ITEM_A, 1),
                    costs = listOf(net.sourceforge.kolmafia.shop.ItemStack(RED_PIXEL, 10)),
                ),
            ),
        )
        val master = CoinmasterDatabase.findByNickname("mystic")!!
        assertEquals(true, CoinmasterPurchaseAccessibility.visitInventoryItemAvailable(master, ITEM_A))
        assertEquals(false, CoinmasterPurchaseAccessibility.visitInventoryItemAvailable(master, ITEM_B))
    }

    @Test
    fun mysticVisitHook_validateUsesOverlayNotBundledRows() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "mystic\tThe Crackpot Mystic's Shed\tCOIN\n",
            coinText = """
                The Crackpot Mystic's Shed\tROW100\tlisted item A\tred pixel (10)
                The Crackpot Mystic's Shed\tROW101\tlisted item B\twhite pixel (10)
            """.trimIndent(),
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "mystic\tThe Crackpot Mystic's Shed\tCOIN\n",
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
        lib.processVisitResponseHooks(
            """
                <tr rel="$ITEM_A">
                <a onClick='javascript:descitem($ITEM_A)'><b>listed item A</b></a>
                <span title="red pixel"><b>10</b></span>
                <form action="shop.php?action=buy&whichshop=mystic&whichrow=100">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=mystic",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($ITEM_A, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($ITEM_B, true));""").trim())
    }

    private fun inventoryWithPixels(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        RED_PIXEL to InventoryItem(RED_PIXEL, "red pixel", 100, ItemType.OTHER),
                        WHITE_PIXEL to InventoryItem(WHITE_PIXEL, "white pixel", 100, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }

    private fun registerItems() {
        listOf(
            ITEM_A to "listed item A",
            ITEM_B to "listed item B",
            RED_PIXEL to "red pixel",
            WHITE_PIXEL to "white pixel",
        ).forEach { (id, name) ->
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = "d$id",
                    image = "img",
                    primaryUse = ItemPrimaryUse.USABLE,
                    secondaryUses = emptySet(),
                    access = setOf('t'),
                    autosellPrice = 1,
                    plural = null,
                ),
            )
        }
    }

    companion object {
        private const val ITEM_A = 99701
        private const val ITEM_B = 99702
        private const val RED_PIXEL = 461
        private const val WHITE_PIXEL = 459
    }
}
