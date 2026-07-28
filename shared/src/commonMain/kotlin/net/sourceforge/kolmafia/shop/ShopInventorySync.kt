package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ShopRequest.parseShopInventory] visit-time row learn + session-log spading output. */
object ShopInventorySync {

    private const val LOG_DIVIDER = "--------------------"

    private val SHOP_ID_PATTERN = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)

    fun parseAndLearn(
        html: String,
        url: String?,
        sessionLogger: SessionLogger? = null,
        force: Boolean = false,
    ) {
        if (url?.contains("ajax=1", ignoreCase = true) == true) return
        if (html.contains(">Uh Oh!</b>")) return
        val shopId = extractShopId(url) ?: return

        val shopRows = ShopRowParser.parseShop(html, includeMeat = true)
        if (shopRows.isEmpty()) return

        val shopName = ShopRowDatabase.shopName(shopId)
        val isConcoctionShop = ShopRowDatabase.shopType(shopId) == ShopType.CONC
        val newShopRows = mutableListOf<ShopRow>()
        val newMeatRows = mutableListOf<ShopRow>()
        val newCoinRows = mutableListOf<ShopRow>()
        val newConcoctionRows = mutableListOf<ShopRow>()

        for (shopRow in shopRows) {
            if (shopRow.costs.isEmpty()) continue
            val existing = ShopRowDatabase.getShopRow(shopRow.rowId)
            if (existing != null && !force) continue

            newShopRows.add(shopRow)
            ShopRowDatabase.registerVisitRow(shopRow.rowId, shopId, shopRow)

            when {
                shopRow.isMeatPurchase -> newMeatRows.add(shopRow)
                isConcoctionShop -> newConcoctionRows.add(shopRow)
                else -> newCoinRows.add(shopRow)
            }
        }

        registerOverlays(shopId, shopName, isConcoctionShop, newMeatRows, newCoinRows, shopRows)

        if (newShopRows.isEmpty()) return
        logLearnedRows(
            sessionLogger = sessionLogger,
            shopId = shopId,
            shopName = shopName,
            newShopRows = newShopRows,
            newMeatRows = newMeatRows,
            newCoinRows = newCoinRows,
            newConcoctionRows = newConcoctionRows,
        )
    }

    private fun registerOverlays(
        shopId: String,
        shopName: String,
        isConcoctionShop: Boolean,
        newMeatRows: List<ShopRow>,
        newCoinRows: List<ShopRow>,
        allParsedRows: List<ShopRow>,
    ) {
        for (shopRow in newMeatRows) {
            val itemName = ItemDatabase.getById(shopRow.item.itemId)?.name ?: continue
            NpcStoreVisitOverlay.registerMeatRow(
                storeKey = shopId,
                storeName = shopName,
                itemId = shopRow.item.itemId,
                itemName = itemName,
                price = shopRow.costs.first().count,
                rowId = shopRow.rowId,
            )
        }

        if (isConcoctionShop) return

        val coinBuyRows = allParsedRows.filter { row ->
            row.costs.isNotEmpty() && !row.isMeatPurchase
        }
        if (coinBuyRows.isNotEmpty() && !CoinmasterVisitInventory.isDynamicShop(shopId)) {
            CoinmasterVisitInventory.registerVisitBuyRows(shopId, coinBuyRows)
        }
    }

    private fun logLearnedRows(
        sessionLogger: SessionLogger?,
        shopId: String,
        shopName: String,
        newShopRows: List<ShopRow>,
        newMeatRows: List<ShopRow>,
        newCoinRows: List<ShopRow>,
        newConcoctionRows: List<ShopRow>,
    ) {
        if (sessionLogger == null) return

        sessionLogger.appendRawLine(LOG_DIVIDER)
        for (shopRow in newShopRows.sortedBy { it.rowId }) {
            sessionLogger.appendRawLine(
                ShopRowDatabase.toData(shopRow.rowId, shopId, shopRow),
            )
        }
        sessionLogger.appendRawLine(LOG_DIVIDER)

        if (newMeatRows.isNotEmpty()) {
            for (shopRow in newMeatRows) {
                val line = NpcStoreVisitOverlay.toNpcStoreLine(shopRow.item.itemId)
                    ?: ItemDatabase.getById(shopRow.item.itemId)?.name?.let { itemName ->
                        "$shopName\t$shopId\t$itemName\t${shopRow.costs.first().count}\tROW${shopRow.rowId}"
                    }
                if (line != null) {
                    sessionLogger.appendRawLine(line)
                }
            }
            sessionLogger.appendRawLine(LOG_DIVIDER)
        }

        if (newConcoctionRows.isNotEmpty()) {
            val craftingType = ShopRowDatabase.craftingType(shopId) ?: "UNKNOWN"
            for (shopRow in newConcoctionRows) {
                sessionLogger.appendRawLine(ShopRowFormatting.toConcoctionData(craftingType, shopRow))
            }
            sessionLogger.appendRawLine(LOG_DIVIDER)
        }

        if (newCoinRows.isNotEmpty()) {
            for (shopRow in newCoinRows) {
                sessionLogger.appendRawLine(ShopRowFormatting.toCoinmasterData(shopName, shopRow))
            }
            sessionLogger.appendRawLine(LOG_DIVIDER)
        }
    }

    internal fun extractShopId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return SHOP_ID_PATTERN.find(url)?.groupValues?.getOrNull(1)?.trim()
    }
}
