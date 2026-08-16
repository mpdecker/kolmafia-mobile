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
import net.sourceforge.kolmafia.shop.MerchTableSync
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRowDatabase
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP206Test {

    @AfterTest
    fun tearDown() {
        CoinmasterDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ShopRowDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase207() {
        assertEquals("phase550", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun conmerch_validateDeniedUntilVisitOverlay() {
        registerConmerchItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = """
                KoL Con 13 Merch Table	buy	1	Twitching Television Tattoo	ROW895
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
                    7567 to InventoryItem(7567, "Chroner", 2000, ItemType.OTHER),
                ),
            ),
        )

        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(${MerchTableSync.TWITCHING_TELEVISION_TATTOO}, true));""").trim(),
        )
        prefs.setBoolean(TimeTowerSync.PREF, true)
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(${MerchTableSync.TWITCHING_TELEVISION_TATTOO}, true));""").trim(),
        )

        ShopInventorySync.parseAndLearn(
            html = conmerchVisitHtml(),
            url = "shop.php?whichshop=conmerch",
            sessionLogger = null,
            prefs = prefs,
        )

        assertTrue(CoinmasterVisitInventory.hasVisited("conmerch"))
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(${MerchTableSync.TWITCHING_TELEVISION_TATTOO}, true));""").trim(),
        )

        CoinmasterVisitInventory.replaceBuyRows(MerchTableSync.SHOP_ID, emptyList())
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(${MerchTableSync.TWITCHING_TELEVISION_TATTOO}, true));""").trim(),
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

    private fun registerConmerchItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = MerchTableSync.TWITCHING_TELEVISION_TATTOO,
                name = "Twitching Television Tattoo",
                descId = "9148",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 7567,
                name = "Chroner",
                descId = "7567",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun conmerchVisitHtml() = """
        <tr rel="${MerchTableSync.TWITCHING_TELEVISION_TATTOO}">
        <a onClick='javascript:descitem(${MerchTableSync.TWITCHING_TELEVISION_TATTOO})'><b>Twitching Television Tattoo</b></a>
        <span title="Chroner"><b>1111</b></span>
        <form action="shop.php?action=buy&whichshop=conmerch&whichrow=895">
        </tr>
    """.trimIndent()
}
