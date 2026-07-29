package net.sourceforge.kolmafia.shop

/** Runtime buy/sell-row overlay for shops that refresh inventory on visit (desktop clears/rebuilds). */
object CoinmasterVisitInventory {

    const val CONMERCH = "conmerch"
    const val SWAGGER = "swagger"
    const val FLOWER_TRADEIN = "flowertradein"
    const val CRIMBO25_SAMMY = "crimbo25_sammy"

    private val dynamicShops = setOf(CONMERCH, SWAGGER, FLOWER_TRADEIN, CRIMBO25_SAMMY)
    private val visitRows = mutableMapOf<String, List<ShopRow>>()
    private val visitSellRows = mutableMapOf<String, List<ShopRow>>()

    fun isDynamicShop(shopId: String): Boolean = shopId.lowercase() in dynamicShops

    fun hasVisited(shopId: String): Boolean {
        val key = shopId.lowercase()
        return visitRows.containsKey(key) || visitSellRows.containsKey(key)
    }

    fun hasVisitOverlay(shopId: String): Boolean =
        visitRows.containsKey(shopId.lowercase())

    fun hasVisitSellOverlay(shopId: String): Boolean =
        visitSellRows.containsKey(shopId.lowercase())

    fun replaceBuyRows(shopId: String, rows: List<ShopRow>) {
        visitRows[shopId.lowercase()] = rows
    }

    fun replaceSellRows(shopId: String, rows: List<ShopRow>) {
        visitSellRows[shopId.lowercase()] = rows
    }

    /** Runtime buy rows learned from generic shop inventory parse (AshP191). */
    fun registerVisitBuyRows(shopId: String, rows: List<ShopRow>) {
        val buyRows = rows.filter { row -> row.costs.isNotEmpty() && !row.isMeatPurchase }
        if (buyRows.isEmpty()) return
        visitRows[shopId.lowercase()] = buyRows
    }

    /** Runtime sell rows learned from legacy coinmaster visit parse (AshP199). */
    fun registerVisitSellRows(shopId: String, rows: List<ShopRow>) {
        if (rows.isEmpty()) return
        visitSellRows[shopId.lowercase()] = rows
    }

    fun findBuyRow(shopId: String, itemId: Int): ShopRow? =
        visitRows[shopId.lowercase()]?.firstOrNull { row ->
            !row.item.isSkill && row.item.itemId == itemId
        }

    fun findBuyRowBySkill(shopId: String, skillId: Int): ShopRow? =
        visitRows[shopId.lowercase()]?.firstOrNull { row ->
            row.item.isSkill && row.item.itemId == skillId
        }

    /** [soldItemId] is the junk item sold to the shop (cost column on visit-learned rows). */
    fun findSellRow(shopId: String, soldItemId: Int): ShopRow? =
        visitSellRows[shopId.lowercase()]?.firstOrNull { row ->
            row.costs.firstOrNull()?.itemId == soldItemId
        }

    fun containsItem(shopId: String, itemId: Int): Boolean = findBuyRow(shopId, itemId) != null

    fun containsSkill(shopId: String, skillId: Int): Boolean = findBuyRowBySkill(shopId, skillId) != null

    fun containsSellItem(shopId: String, soldItemId: Int): Boolean =
        findSellRow(shopId, soldItemId) != null

    internal fun resetForTest() {
        visitRows.clear()
        visitSellRows.clear()
    }
}
