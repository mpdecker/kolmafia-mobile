package net.sourceforge.kolmafia.shop

import kotlin.test.*
import kotlinx.coroutines.runBlocking

class CoinmasterRegistryTest {

    @Test
    fun registry_isNonEmpty() {
        assertTrue(CoinmasterRegistry.all.isNotEmpty())
    }

    @Test
    fun findByNickname_dmt_returnsDimemaster() {
        val master = CoinmasterRegistry.findByNickname("dmt")
        assertNotNull(master)
        assertEquals("Dimemaster", master.masterName)
    }

    @Test
    fun findByShopId_returnsSameMaster() {
        val shore = CoinmasterRegistry.findByNickname("shore")!!
        val byId = CoinmasterRegistry.findByShopId(shore.shopId!!)
        assertNotNull(byId)
        assertEquals(shore.masterName, byId.masterName)
    }

    @Test
    fun findByNickname_unknown_returnsNull() {
        assertNull(CoinmasterRegistry.findByNickname("notacoinmaster"))
    }

    @Test
    fun dimemaster_hasBuyItems() {
        val dmt = CoinmasterRegistry.findByNickname("dmt")!!
        assertTrue(dmt.buyItems.isNotEmpty())
    }

    @Test
    fun dimemasterAndQuartersmaster_useCampActions() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val dimemaster = CoinmasterRegistry.findByNickname("dimemaster")!!
        val quartersmaster = CoinmasterRegistry.findByNickname("quartersmaster")!!
        assertEquals("getgear", dimemaster.buyAction)
        assertEquals("turnin", dimemaster.sellAction)
        assertTrue(dimemaster.useItemField)
        assertEquals("getgear", quartersmaster.buyAction)
        assertEquals("turnin", quartersmaster.sellAction)
        assertTrue(quartersmaster.useItemField)
    }

    @Test
    fun shopRow_meatCost_isMeatTrue() {
        val row = ShopRow(
            rowId = 1,
            item = ItemStack(itemId = 1, count = 1),
            costs = listOf(ItemStack(itemId = -1, count = 500, isMeat = true))
        )
        assertTrue(row.isMeatPurchase)
    }

    @Test
    fun shopRow_tokenCost_isMeatFalse() {
        val row = ShopRow(
            rowId = 1,
            item = ItemStack(itemId = 1, count = 1),
            costs = listOf(ItemStack(itemId = 46, count = 1, isMeat = false))
        )
        assertFalse(row.isMeatPurchase)
    }
}
