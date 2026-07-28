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
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRow
import net.sourceforge.kolmafia.shop.ShopRowDatabase
import net.sourceforge.kolmafia.shop.ItemStack

class GameRuntimeLibraryAshP192Test {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun visitLearnedFdkolRow_validateTrueWhenPresent() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "FDKOL Requisitions Tent\tbuy\t75\tvisit-learned item\tROW1500\n",
        )
        CoinmasterVisitInventory.registerVisitBuyRows(
            "fdkol",
            listOf(
                ShopRow(
                    rowId = 1500,
                    item = ItemStack(itemId = VISIT_ITEM, count = 1),
                    costs = listOf(ItemStack(itemId = FDKOL_COMMENDATION, count = 75)),
                ),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithCommendations(),
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item($VISIT_ITEM, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(${VISIT_ITEM + 1}, true));""").trim())
    }

    @Test
    fun shopVisit_populatesVisitOverlayForValidate() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "",
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            """
                <tr rel="$VISIT_ITEM">
                <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
                <span title="FDKOL commendation"><b>75</b></span>
                <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=fdkol",
        )
        assertEquals(true, CoinmasterVisitInventory.containsItem("fdkol", VISIT_ITEM))
    }

    private fun inventoryWithCommendations(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        FDKOL_COMMENDATION to InventoryItem(
                            FDKOL_COMMENDATION,
                            "FDKOL commendation",
                            100,
                            ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }

    private fun registerItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = VISIT_ITEM,
                name = "visit-learned item",
                descId = "d$VISIT_ITEM",
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
                id = VISIT_ITEM + 1,
                name = "other item",
                descId = "d${VISIT_ITEM + 1}",
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
                id = FDKOL_COMMENDATION,
                name = "FDKOL commendation",
                descId = "d$FDKOL_COMMENDATION",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val VISIT_ITEM = 99301
        private const val FDKOL_COMMENDATION = 99302
    }
}
