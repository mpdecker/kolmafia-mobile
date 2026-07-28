package net.sourceforge.kolmafia.shop

/** Runtime buy-row overlay for shops that refresh inventory on visit (desktop clears/rebuilds). */
object CoinmasterVisitInventory {

    const val CONMERCH = "conmerch"
    const val SWAGGER = "swagger"
    const val FLOWER_TRADEIN = "flowertradein"
    const val CRIMBO25_SAMMY = "crimbo25_sammy"

    private val dynamicShops = setOf(CONMERCH, SWAGGER, FLOWER_TRADEIN, CRIMBO25_SAMMY)
    private val visitRows = mutableMapOf<String, List<ShopRow>>()

    fun isDynamicShop(shopId: String): Boolean = shopId.lowercase() in dynamicShops

    fun hasVisited(shopId: String): Boolean = visitRows.containsKey(shopId.lowercase())

    fun hasVisitOverlay(shopId: String): Boolean =
        visitRows[shopId.lowercase()]?.isNotEmpty() == true

    fun replaceBuyRows(shopId: String, rows: List<ShopRow>) {
        visitRows[shopId.lowercase()] = rows
    }

    /** Runtime buy rows learned from generic shop inventory parse (AshP191). */
    fun registerVisitBuyRows(shopId: String, rows: List<ShopRow>) {
        val buyRows = rows.filter { row -> row.costs.isNotEmpty() && !row.isMeatPurchase }
        if (buyRows.isEmpty()) return
        visitRows[shopId.lowercase()] = buyRows
    }

    fun findBuyRow(shopId: String, itemId: Int): ShopRow? =
        visitRows[shopId.lowercase()]?.firstOrNull { it.item.itemId == itemId }

    fun containsItem(shopId: String, itemId: Int): Boolean = findBuyRow(shopId, itemId) != null

    internal fun resetForTest() {
        visitRows.clear()
    }
}
