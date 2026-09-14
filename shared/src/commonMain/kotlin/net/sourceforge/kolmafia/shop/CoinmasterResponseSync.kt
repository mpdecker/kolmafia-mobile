package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest.parseResponse] visit/buy/sell glue. */
object CoinmasterResponseSync {

    private val TOKEN_BALANCE_PATTERN =
        Regex("""You have (\d[\d,]*)\s+""", RegexOption.IGNORE_CASE)

    fun apply(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager?,
        character: KoLCharacter?,
        gameDatabase: GameDatabase? = null,
    ): Boolean {
        if (!url.contains("shop.php", ignoreCase = true)) return false
        val shopId = ShopInventorySync.extractShopId(url) ?: return false
        val master = CoinmasterRegistry.findByShopId(shopId) ?: return false

        val action = queryParam(url, "action")
        if (action.isNullOrBlank()) {
            parseBalance(master, html, preferences, inventory)
            return true
        }

        when {
            isBuyAction(action, master) -> {
                if (html.contains("You don't have enough") || html.contains("Huh?")) {
                    parseBalance(master, html, preferences, inventory)
                    return true
                }
                if (master.hasShopRowInventory()) {
                    val row = findBuyRow(master, url) ?: return true
                    val count = extractCount(url).coerceAtLeast(1)
                    completePurchaseShopRow(master, row, count, preferences, inventory, gameDatabase)
                } else {
                    completePurchaseLegacy(master, url, preferences, inventory, gameDatabase)
                }
            }
            isSellAction(action, master) -> {
                if (html.contains("You don't have that many")) {
                    parseBalance(master, html, preferences, inventory)
                    return true
                }
                if (master.hasShopRowInventory()) {
                    val row = findSellRow(master, url) ?: return true
                    val count = extractCount(url).coerceAtLeast(1)
                    completeSaleShopRow(master, row, count, preferences, inventory)
                } else {
                    completeSaleLegacy(master, url, preferences, inventory)
                }
            }
        }

        parseBalance(master, html, preferences, inventory)
        markConcoctionRefreshIfPseudoToken(master)
        return true
    }

    internal fun parseBalance(
        master: CoinmasterData,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager?,
    ) {
        if (preferences == null) return
        if (master.hasShopRowInventory()) return

        val property = master.property ?: return
        val token = master.token ?: return
        val match = TOKEN_BALANCE_PATTERN.find(html) ?: return
        val tail = html.substring(match.range.last + 1)
        if (!tail.startsWith(token, ignoreCase = true)) return

        val balance = match.groupValues[1].replace(",", "").toIntOrNull() ?: return
        preferences.setInt(property, balance)

        val tokenItemId = master.tokenItemId() ?: return
        val current = inventory?.state?.value?.items?.get(tokenItemId)?.quantity ?: 0
        val delta = balance - current
        when {
            delta > 0 -> inventory?.gainItemLocally(tokenItemId, delta)
            delta < 0 -> inventory?.consumeItemLocally(tokenItemId, -delta)
        }
    }

    private fun completePurchaseShopRow(
        master: CoinmasterData,
        row: ShopRow,
        count: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
        gameDatabase: GameDatabase? = null,
    ) {
        val property = master.property
        val propertyOnly = property != null && master.tokenItemId() == null
        var propertyUnits = 0
        for (cost in row.costs) {
            val price = cost.count * count
            if (cost.isMeat) continue
            if (cost.itemId > 0) {
                inventory?.consumeItemLocally(cost.itemId, price)
            } else if (propertyOnly) {
                propertyUnits += price
            }
        }
        if (propertyOnly) {
            if (row.costs.isEmpty()) {
                propertyUnits = count
            }
            if (propertyUnits > 0 && preferences != null && property != null) {
                preferences.setInt(
                    property,
                    (preferences.getInt(property, 0) - propertyUnits).coerceAtLeast(0),
                )
            }
        }
        if (!row.item.isSkill) {
            inventory?.gainItemLocally(row.item.itemId, row.item.count * count)
            CoinmasterPurchasePrefs.applyPurchasedItem(master, row.item.itemId, preferences, gameDatabase)
        }
        markConcoctionRefreshIfPseudoToken(master)
    }

    private fun completePurchaseLegacy(
        master: CoinmasterData,
        url: String,
        preferences: Preferences?,
        inventory: InventoryManager?,
        gameDatabase: GameDatabase? = null,
    ) {
        val row = findBuyRow(master, url) ?: return
        val itemId = row.item.itemId
        val count = extractCount(url).coerceAtLeast(1)
        val unitPrice = row.costs.firstOrNull()?.count?.takeIf { it > 0 } ?: row.price
        if (unitPrice <= 0) return

        deductTokenCost(master, unitPrice * count, preferences, inventory)
        if (!row.item.isSkill) {
            inventory?.gainItemLocally(itemId, count)
            CoinmasterPurchasePrefs.applyPurchasedItem(master, itemId, preferences, gameDatabase)
        }
        markConcoctionRefreshIfPseudoToken(master)
    }

    private fun completeSaleShopRow(
        master: CoinmasterData,
        row: ShopRow,
        count: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
    ) {
        inventory?.consumeItemLocally(row.item.itemId, row.item.count * count)
        val credit = row.price * count
        if (credit > 0) {
            creditTokenGain(master, credit, preferences, inventory)
        }
        markConcoctionRefreshIfPseudoToken(master)
    }

    private fun completeSaleLegacy(
        master: CoinmasterData,
        url: String,
        preferences: Preferences?,
        inventory: InventoryManager?,
    ) {
        val row = findSellRow(master, url) ?: return
        val itemId = row.item.itemId
        val count = extractCount(url).coerceAtLeast(1)
        val credit = row.price * count

        inventory?.consumeItemLocally(itemId, count)
        if (credit > 0) {
            creditTokenGain(master, credit, preferences, inventory)
        }
        markConcoctionRefreshIfPseudoToken(master)
    }

    private fun deductTokenCost(
        master: CoinmasterData,
        cost: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
    ) {
        val property = master.property
        val tokenItemId = master.tokenItemId()
        when {
            property != null && tokenItemId == null -> {
                preferences?.let { prefs ->
                    val current = prefs.getInt(property, 0)
                    prefs.setInt(property, (current - cost).coerceAtLeast(0))
                }
            }
            tokenItemId != null -> inventory?.consumeItemLocally(tokenItemId, cost)
        }
    }

    private fun creditTokenGain(
        master: CoinmasterData,
        credit: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
    ) {
        val property = master.property
        val tokenItemId = master.tokenItemId()
        when {
            property != null -> {
                preferences?.let { prefs ->
                    prefs.setInt(property, prefs.getInt(property, 0) + credit)
                }
            }
            tokenItemId != null -> inventory?.gainItemLocally(tokenItemId, credit)
        }
    }

    private fun markConcoctionRefreshIfPseudoToken(master: CoinmasterData) {
        if (master.tokenItemId() == null && master.property != null) {
            ConcoctionDatabase.markRefreshNeeded()
        }
    }

    private fun isBuyAction(action: String, master: CoinmasterData): Boolean =
        action.equals("buyitem", ignoreCase = true) ||
            action.equals(master.buyAction, ignoreCase = true)

    private fun isSellAction(action: String, master: CoinmasterData): Boolean =
        action.equals("sellitem", ignoreCase = true) ||
            action.equals(master.sellAction, ignoreCase = true)

    private fun findBuyRow(master: CoinmasterData, url: String): ShopRow? {
        val rowId = queryParam(url, "whichrow")?.toIntOrNull() ?: return null
        return master.buyItems.firstOrNull { it.rowId == rowId }
    }

    private fun findSellRow(master: CoinmasterData, url: String): ShopRow? {
        val rowId = queryParam(url, "whichrow")?.toIntOrNull() ?: return null
        return master.sellItems.firstOrNull { it.rowId == rowId }
    }

    private fun extractCount(url: String): Int =
        queryParam(url, "quantity")?.toIntOrNull()?.takeIf { it > 0 } ?: 1

    private fun queryParam(url: String, key: String): String? {
        val qIndex = url.indexOf('?')
        val query = if (qIndex >= 0) url.substring(qIndex + 1) else return null
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq < 0) continue
            if (part.substring(0, eq).equals(key, ignoreCase = true)) {
                return decodeQueryValue(part.substring(eq + 1))
            }
        }
        return null
    }

    private fun decodeQueryValue(value: String): String =
        value.replace('+', ' ')
            .replace("%20", " ", ignoreCase = true)
            .replace("%26", "&", ignoreCase = true)
}
