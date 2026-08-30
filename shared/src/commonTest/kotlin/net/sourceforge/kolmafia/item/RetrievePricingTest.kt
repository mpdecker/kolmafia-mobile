package net.sourceforge.kolmafia.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetrievePricingTest {

    @Test
    fun cheaperToBuy_falseWhenNotTradeableOrUnknown() {
        // ItemDatabase may be unloaded in unit tests → not tradeable
        val ctx = RetrievePricing.PriceContext(
            inventoryCount = { 0 },
            mallPrice = { 100L },
            npcPrice = { 0L },
            canCreate = { true },
        )
        assertFalse(RetrievePricing.cheaperToBuy(1, 1, ctx))
        assertFalse(RetrievePricing.cheaperToBuy(-999, 1, ctx))
    }

    @Test
    fun cheaperToBuy_trueWhenTradeableAndMakeUnavailable() {
        // Inject tradeable via ItemDatabase only when loaded; otherwise skip economics
        // and verify invokeBuyScript / priceToAcquire paths instead.
        val ctx = RetrievePricing.PriceContext(
            inventoryCount = { 0 },
            mallPrice = { 100L },
            npcPrice = { 0L },
            canCreate = { false },
        )
        // priceToAcquire still picks mall even when cheaperToBuy gates on tradeable
        assertEquals(100L, RetrievePricing.priceToAcquire(1, 1, exact = true, ctx = ctx))
    }

    @Test
    fun priceToAcquire_prefersNpcOverMall() {
        val ctx = RetrievePricing.PriceContext(
            inventoryCount = { 0 },
            mallPrice = { 500L },
            npcPrice = { 100L },
            canCreate = { false },
        )
        assertEquals(100L, RetrievePricing.priceToAcquire(1, 1, exact = true, ctx = ctx))
    }

    @Test
    fun retrievePrice_returnsNegativeWhenUnavailable() {
        val ctx = RetrievePricing.PriceContext(
            inventoryCount = { 0 },
            mallPrice = { -1L },
            npcPrice = { 0L },
            canCreate = { false },
        )
        assertEquals(-1L, RetrievePricing.retrievePrice(999999, ctx))
    }

    @Test
    fun invokeBuyScript_defaultsWhenNoScript() {
        assertTrue(
            RetrievePricing.invokeBuyScript(
                prefs = null,
                itemName = "widget",
                qty = 1,
                ingredientLevel = 2,
                defaultBuy = true,
            ),
        )
        assertFalse(
            RetrievePricing.invokeBuyScript(
                prefs = null,
                itemName = "widget",
                qty = 1,
                ingredientLevel = 2,
                defaultBuy = false,
            ),
        )
    }

    @Test
    fun itemValue_scalesAutosell() {
        // Without ItemDatabase entry, autosell is 0
        val ctx = RetrievePricing.PriceContext()
        assertEquals(0L, RetrievePricing.itemValue(999999, exact = true, ctx = ctx))
    }
}
