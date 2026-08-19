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
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility
import net.sourceforge.kolmafia.shop.FlowerTradeinSync
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRowDatabase

class GameRuntimeLibraryAshP204Test {

    @AfterTest
    fun tearDown() {
        CoinmasterDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ShopRowDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase207() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun disabledCoinmaster_validateDeniedUntilVisitOverlay() {
        registerDisabledShopItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "disabledshop\tDisabled Test Shop\tCOIN\n",
            coinText = """
                Disabled Test Shop	buy	75	disabled widget	ROW1500
            """.trimIndent(),
        )
        CoinmasterDatabase.findByShopId("disabledshop")?.setDisabledForTest(true)

        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWith(mapOf(TOKEN to InventoryItem(TOKEN, "disabled token", 100, ItemType.OTHER))),
        )

        assertEquals("false", outputLib(lib, """print(is_coinmaster_item($WIDGET, true));""").trim())

        ShopInventorySync.parseAndLearn(
            html = disabledVisitHtml(),
            url = "shop.php?whichshop=disabledshop",
            sessionLogger = null,
        )

        assertTrue(CoinmasterVisitInventory.hasVisited("disabledshop"))
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item($WIDGET, true));""").trim(),
        )
    }

    @Test
    fun flowertradein_visitShopRowsHook_authorityWithoutCoinmasterShopSync() {
        registerFlowerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = """
                The Central Loathing Floral Mercantile Exchange	buy	1	Chroner	ROW759
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWith(
                mapOf(
                    FlowerTradeinAccessibility.ROSE to InventoryItem(
                        FlowerTradeinAccessibility.ROSE,
                        "rose",
                        2,
                        ItemType.OTHER,
                    ),
                ),
            ),
        )

        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(${FlowerTradeinSync.CHRONER}, true));""").trim())

        ShopInventorySync.parseAndLearn(
            html = flowerVisitHtml(),
            url = "shop.php?whichshop=flowertradein",
            sessionLogger = null,
        )

        assertFalse(CoinmasterVisitInventory.findBuyRow(FlowerTradeinSync.SHOP_ID, FlowerTradeinSync.CHRONER) == null)
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(${FlowerTradeinSync.CHRONER}, true));""").trim(),
        )

        CoinmasterVisitInventory.replaceBuyRows(FlowerTradeinSync.SHOP_ID, emptyList())
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(${FlowerTradeinSync.CHRONER}, true));""").trim(),
        )
    }

    private fun inventoryWith(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
        }

    private fun registerDisabledShopItems() {
        listOf(
            WIDGET to "disabled widget",
            TOKEN to "disabled token",
        ).forEach { (id, name) ->
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = id.toString(),
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

    private fun registerFlowerItems() {
        listOf(
            FlowerTradeinSync.CHRONER to "Chroner",
            FlowerTradeinAccessibility.ROSE to "rose",
        ).forEach { (id, name) ->
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = id.toString(),
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

    private fun disabledVisitHtml() = """
        <tr rel="$WIDGET">
        <a onClick='javascript:descitem($WIDGET)'><b>disabled widget</b></a>
        <span title="disabled token"><b>75</b></span>
        <form action="shop.php?action=buy&whichshop=disabledshop&whichrow=1500">
        </tr>
    """.trimIndent()

    private fun flowerVisitHtml() = """
        <tr rel="${FlowerTradeinSync.CHRONER}">
        <a onClick='javascript:descitem(${FlowerTradeinSync.CHRONER})'><b>Chroner</b></a>
        <span title="rose"><b>1</b></span>
        <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
        </tr>
    """.trimIndent()

    companion object {
        private const val WIDGET = 99401
        private const val TOKEN = 99402
    }
}
