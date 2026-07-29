package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ShopRequest.parseShopInventory] visit-time row learn + session-log spading output. */
object ShopInventorySync {

    private const val LOG_DIVIDER = "--------------------"

    private val SHOP_ID_PATTERN = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)
    private val SHOP_NAME_PATTERN = Regex(
        """<table.*?<b.*?>(.*?)</b>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private enum class RowBucket {
        MEAT,
        CONC,
        LEGACY_BUY,
        LEGACY_SELL,
        COIN,
    }

    fun parseAndLearn(
        html: String,
        url: String?,
        sessionLogger: SessionLogger? = null,
        prefs: Preferences? = null,
        state: CharacterState? = null,
        force: Boolean = false,
        onNewSkillRegistered: ((Int) -> Unit)? = null,
    ) {
        if (url?.contains("ajax=1", ignoreCase = true) == true) return
        if (html.contains(">Uh Oh!</b>")) return
        val shopId = extractShopId(url) ?: return

        val newlyRegisteredSkillIds = mutableListOf<Int>()
        val shopRows = ShopRowParser.parseShop(
            html = html,
            includeMeat = true,
            newlyRegisteredSkillIds = newlyRegisteredSkillIds,
        )

        val parsedName = parseShopNameFromHtml(html)
        if (parsedName.isNotEmpty() && ShopRowDatabase.registerShop(shopId, parsedName, sessionLogger = sessionLogger)) {
            sessionLogger?.appendRawLine("New shop: ($shopId, \"$parsedName\")")
        }
        val shopName = parsedName.ifEmpty { ShopRowDatabase.shopName(shopId) }

        if (isVisitOnly(url) && ShopRowDatabase.logVisits(shopId)) {
            sessionLogger?.appendRawLine("Visiting $shopName")
        }

        val shopType = ShopRowDatabase.shopType(shopId)
        val isConcoctionShop = shopType == ShopType.CONC
        val coinmaster = CoinmasterDatabase.findByShopId(shopId)
        val disabled = coinmaster?.isDisabled == true
        val currencies = coinmaster?.currencyItemIds() ?: emptySet()

        coinmaster?.visitShopRows?.invoke(shopRows, force, sessionLogger)
        coinmaster?.visitShop?.invoke(html, url, prefs, sessionLogger, state)

        if (shopRows.isEmpty()) return

        val newShopRows = mutableListOf<ShopRow>()
        val newMeatRows = mutableListOf<ShopRow>()
        val newBuyRows = mutableListOf<ShopRow>()
        val newSellRows = mutableListOf<ShopRow>()
        val newCoinRows = mutableListOf<ShopRow>()
        val newConcoctionRows = mutableListOf<ShopRow>()

        for (shopRow in shopRows) {
            if (shopRow.costs.isEmpty()) continue
            val existing = if (disabled) null else ShopRowDatabase.getShopRow(shopRow.rowId)
            if (existing != null && !force) continue

            newShopRows.add(shopRow)
            ShopRowDatabase.registerVisitRow(shopRow.rowId, shopId, shopRow)
            prefs?.let { ShopRowDatabase.persistLearnedRow(it, shopRow.rowId, shopId, shopRow) }

            when (classifyRow(shopRow, isConcoctionShop, shopType, coinmaster, currencies, disabled)) {
                RowBucket.MEAT -> newMeatRows.add(shopRow)
                RowBucket.CONC -> newConcoctionRows.add(shopRow)
                RowBucket.LEGACY_BUY -> newBuyRows.add(shopRow)
                RowBucket.LEGACY_SELL -> newSellRows.add(shopRow)
                RowBucket.COIN -> newCoinRows.add(shopRow)
            }
        }

        val inferredType = when {
            coinmaster != null -> ShopType.COIN
            newMeatRows.isNotEmpty() -> ShopType.NPC
            else -> ShopType.NONE
        }
        if (inferredType != ShopType.NONE) {
            ShopRowDatabase.promoteShopType(shopId, inferredType, sessionLogger)
        }

        registerOverlays(
            shopId = shopId,
            shopName = shopName,
            isConcoctionShop = isConcoctionShop,
            newMeatRows = newMeatRows,
            newBuyRows = newBuyRows,
            newSellRows = newSellRows,
            newCoinRows = newCoinRows,
            allParsedRows = shopRows,
            currencies = currencies,
        )

        if (newShopRows.isEmpty()) return
        for (skillId in newlyRegisteredSkillIds) {
            onNewSkillRegistered?.invoke(skillId)
        }
        logLearnedRows(
            sessionLogger = sessionLogger,
            shopId = shopId,
            shopName = shopName,
            newShopRows = newShopRows,
            newMeatRows = newMeatRows,
            newBuyRows = newBuyRows,
            newSellRows = newSellRows,
            newCoinRows = newCoinRows,
            newConcoctionRows = newConcoctionRows,
        )
    }

    internal fun parseShopNameFromHtml(html: String): String =
        SHOP_NAME_PATTERN.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()

    internal fun isVisitOnly(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        val action = Regex("""[?&]action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.trim()
        return action.isNullOrEmpty() || !action.equals("buyitem", ignoreCase = true)
    }

    private fun classifyRow(
        shopRow: ShopRow,
        isConcoctionShop: Boolean,
        shopType: ShopType,
        coinmaster: CoinmasterData?,
        currencies: Set<Int>,
        disabled: Boolean,
    ): RowBucket {
        if (shopRow.isSkillPurchase) return RowBucket.COIN
        if (shopRow.isMeatPurchase) return RowBucket.MEAT

        if (!disabled && shopRow.costs.size == 1 && coinmaster != null && !coinmaster.isShopRowCoinmaster()) {
            val cost = shopRow.costs[0]
            if (coinmaster.buyItems.isNotEmpty() && currencies.contains(cost.itemId)) {
                return RowBucket.LEGACY_BUY
            }
            if (coinmaster.sellItems.isNotEmpty() && currencies.contains(shopRow.item.itemId)) {
                return RowBucket.LEGACY_SELL
            }
        }

        if (isConcoctionShop) return RowBucket.CONC

        val unknown = shopType == ShopType.NONE
        val newStyle = unknown || disabled || coinmaster?.hasShopRowInventory() == true
        if (newStyle) return RowBucket.COIN

        return RowBucket.COIN
    }

    private fun registerOverlays(
        shopId: String,
        shopName: String,
        isConcoctionShop: Boolean,
        newMeatRows: List<ShopRow>,
        newBuyRows: List<ShopRow>,
        newSellRows: List<ShopRow>,
        newCoinRows: List<ShopRow>,
        allParsedRows: List<ShopRow>,
        currencies: Set<Int>,
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

        if (newSellRows.isNotEmpty()) {
            CoinmasterVisitInventory.registerVisitSellRows(shopId, newSellRows)
        }

        val coinBuyRows = allParsedRows.filter { row ->
            row.costs.isNotEmpty() &&
                !row.isMeatPurchase &&
                !(currencies.isNotEmpty() && currencies.contains(row.item.itemId))
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
        newBuyRows: List<ShopRow>,
        newSellRows: List<ShopRow>,
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
            return
        }

        if (newCoinRows.isNotEmpty()) {
            for (shopRow in newCoinRows) {
                sessionLogger.appendRawLine(ShopRowFormatting.toCoinmasterData(shopName, shopRow))
            }
            sessionLogger.appendRawLine(LOG_DIVIDER)
            return
        }

        if (newBuyRows.isNotEmpty()) {
            for (shopRow in newBuyRows) {
                sessionLogger.appendRawLine(ShopRowFormatting.toLegacyBuyData(shopName, shopRow))
            }
            sessionLogger.appendRawLine(LOG_DIVIDER)
        }

        if (newSellRows.isNotEmpty()) {
            for (shopRow in newSellRows) {
                sessionLogger.appendRawLine(ShopRowFormatting.toLegacySellData(shopName, shopRow))
            }
            sessionLogger.appendRawLine(LOG_DIVIDER)
        }
    }

    internal fun extractShopId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return SHOP_ID_PATTERN.find(url)?.groupValues?.getOrNull(1)?.trim()
    }
}
