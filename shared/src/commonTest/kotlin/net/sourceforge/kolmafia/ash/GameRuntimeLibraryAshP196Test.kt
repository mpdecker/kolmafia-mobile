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
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility
import net.sourceforge.kolmafia.shop.FlowerTradeinSync

class GameRuntimeLibraryAshP196Test {

    @Test
    fun revision_phase202() {
        assertEquals("phase475", GameRuntimeLibrary.REVISION)
    }

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun visitOverlayAuthority_blocksBundledChronerRowNotInVisitInventory() {
        registerChronerShop()
        FlowerTradeinSync.syncFromShopHtml(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            Preferences(MapSettings()),
        )
        val master = CoinmasterDatabase.findByNickname("flowertradein")!!
        assertTrue(
            CoinmasterPurchaseAccessibility.visitInventoryItemAvailable(
                master,
                FlowerTradeinSync.CHRONER,
            ),
        )
        CoinmasterVisitInventory.replaceBuyRows(FlowerTradeinSync.SHOP_ID, emptyList())
        assertFalse(
            CoinmasterPurchaseAccessibility.visitInventoryItemAvailable(
                master,
                FlowerTradeinSync.CHRONER,
            ),
        )
    }

    @Test
    fun flowerTradeinVisitHook_validateUsesOverlayNotBundledRows() {
        registerChronerShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithRose(),
        )
        lib.processVisitResponseHooks(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=flowertradein",
        )
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

    @Test
    fun flowerTradeinValidate_blockedWithoutTradeFlower() {
        registerChronerShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        FlowerTradeinSync.syncFromShopHtml(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            prefs,
        )
        val master = CoinmasterDatabase.findByNickname("flowertradein")!!
        val char = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                FlowerTradeinSync.CHRONER,
                char,
                prefs,
                accessibleCount = { 0 },
            ),
        )
        assertEquals(
            "You have no roses or tulips",
            CoinmasterAccessibility.inaccessibleReason(master, char, accessibleCount = { 0 }),
        )
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                FlowerTradeinSync.CHRONER,
                char,
                prefs,
                accessibleCount = { if (it == FlowerTradeinAccessibility.ROSE) 1 else 0 },
            ),
        )
    }

    private fun inventoryWithRose(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        FlowerTradeinAccessibility.ROSE to InventoryItem(
                            FlowerTradeinAccessibility.ROSE,
                            "rose",
                            1,
                            ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }

    private fun registerChronerShop() {
        runBlocking { ItemDatabase.load() }
        CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = """
                The Central Loathing Floral Mercantile Exchange	buy	1	Chroner	ROW759
                The Central Loathing Floral Mercantile Exchange	buy	16	Chroner	ROW760
                The Central Loathing Floral Mercantile Exchange	buy	21	Chroner	ROW761
                The Central Loathing Floral Mercantile Exchange	buy	11	Chroner	ROW762
            """.trimIndent(),
        )
    }
}
