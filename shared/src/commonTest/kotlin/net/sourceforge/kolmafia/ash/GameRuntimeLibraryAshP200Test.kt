package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRow
import net.sourceforge.kolmafia.shop.ShopRowDatabase

class GameRuntimeLibraryAshP200Test {

    @Test
    fun revision_phase207() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun visitSellOverlayAuthority_blocksBundledSellNotInVisitInventory() {
        registerSellShop()
        syncSellVisit()
        val master = CoinmasterDatabase.findByNickname("legacysell")!!
        assertTrue(CoinmasterPurchaseAccessibility.visitInventorySellAvailable(master, JUNK_ITEM))
        CoinmasterVisitInventory.replaceSellRows(SHOP_ID, emptyList())
        assertFalse(CoinmasterPurchaseAccessibility.visitInventorySellAvailable(master, JUNK_ITEM))
    }

    @Test
    fun containsSellItem_validateRequiresInventory() {
        registerSellShop()
        syncSellVisit()
        assertTrue(
            CoinmasterDatabase.containsSellItem(
                itemId = JUNK_ITEM,
                validate = true,
                accessibleCount = { if (it == JUNK_ITEM) 1 else 0 },
            ),
        )
        assertFalse(
            CoinmasterDatabase.containsSellItem(
                itemId = JUNK_ITEM,
                validate = true,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun containsSellItem_validateFalseWhenNotInVisitOverlay() {
        registerSellShop()
        syncSellVisit()
        CoinmasterVisitInventory.replaceSellRows(SHOP_ID, emptyList())
        assertFalse(
            CoinmasterDatabase.containsSellItem(
                itemId = JUNK_ITEM,
                validate = true,
                accessibleCount = { 1 },
            ),
        )
    }

    private fun syncSellVisit() {
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "$SHOP_ID\tLegacy Sell Shop\tCOIN\n",
        )
        ShopInventorySync.parseAndLearn(
            html = sellVisitHtml(),
            url = "shop.php?whichshop=$SHOP_ID",
            sessionLogger = null,
        )
    }

    private fun sellVisitHtml() = """
        <tr rel="$DIME_ITEM">
        <a onClick='javascript:descitem($DIME_ITEM)'><b>legacy dime</b></a>
        <span title="legacy junk"><b>3</b></span>
        <form action="shop.php?action=sell&whichshop=$SHOP_ID&whichrow=2400">
        </tr>
    """.trimIndent()

    private fun registerSellShop() {
        ItemDatabase.registerForTest(
            ItemData(
                id = DIME_ITEM,
                name = "legacy dime",
                descId = DIME_ITEM.toString(),
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
                id = JUNK_ITEM,
                name = "legacy junk",
                descId = JUNK_ITEM.toString(),
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Legacy Sell Shop",
                nickname = "legacysell",
                shopId = SHOP_ID,
                token = "legacy dime",
                buyItems = emptyList(),
                sellItems = listOf(
                    ShopRow(
                        rowId = 99,
                        item = ItemStack(itemId = JUNK_ITEM, count = 1),
                        price = 3,
                    ),
                ),
            ),
        )
    }

    companion object {
        private const val SHOP_ID = "legacysell"
        private const val DIME_ITEM = 99310
        private const val JUNK_ITEM = 99311
    }
}
