package net.sourceforge.kolmafia.shop

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class ShopRowDatabaseTest {

    @AfterTest
    fun cleanup() {
        ShopRowDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun loadFromText_parsesShopRowAndMeatCost() {
        registerItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = """
                3	fdkol	FDKOL tattoo	FDKOL commendation (100)
                10	fdkol	FDKOL hotcakes	5,000 Meat
            """.trimIndent(),
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
        )

        val tattooRow = ShopRowDatabase.getShopRow(3)
        assertNotNull(tattooRow)
        assertEquals(FDKOL_TATTOO, tattooRow.item.itemId)
        assertEquals(1, tattooRow.costs.size)
        assertEquals(FDKOL_COMMENDATION, tattooRow.costs[0].itemId)
        assertEquals(100, tattooRow.costs[0].count)

        val hotcakesRow = ShopRowDatabase.getShopRow(10)
        assertNotNull(hotcakesRow)
        assertTrue(hotcakesRow.costs[0].isMeat)
        assertEquals(5000, hotcakesRow.costs[0].count)
        assertEquals("FDKOL Requisitions Tent", ShopRowDatabase.shopName("fdkol"))
    }

    @Test
    fun loadFromText_parsesShopTypeAndCraftingType() {
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = """
                still	Nash Crosby's Still	CONC	STILL
                fdkol	FDKOL Requisitions Tent	NPCCOIN
            """.trimIndent(),
        )
        assertEquals(ShopType.CONC, ShopRowDatabase.shopType("still"))
        assertEquals("STILL", ShopRowDatabase.craftingType("still"))
        assertEquals(ShopType.NPCCOIN, ShopRowDatabase.shopType("fdkol"))
        assertEquals(null, ShopRowDatabase.craftingType("fdkol"))
    }

    @Test
    fun registerVisitRow_overridesBundledLookup() {
        registerItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val visitRow = ShopRow(
            rowId = 999,
            item = ItemStack(itemId = FDKOL_TATTOO, count = 1),
            costs = listOf(ItemStack(itemId = FDKOL_COMMENDATION, count = 50)),
        )
        ShopRowDatabase.registerVisitRow(999, "fdkol", visitRow)
        assertEquals(50, ShopRowDatabase.getShopRow(999)?.costs?.first()?.count)
    }

    private fun registerItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = FDKOL_TATTOO,
                name = "FDKOL tattoo",
                descId = "d$FDKOL_TATTOO",
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
        ItemDatabase.registerForTest(
            ItemData(
                id = FDKOL_HOTCAKES,
                name = "FDKOL hotcakes",
                descId = "d$FDKOL_HOTCAKES",
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
        private const val FDKOL_TATTOO = 99001
        private const val FDKOL_COMMENDATION = 99002
        private const val FDKOL_HOTCAKES = 99003
    }
}
