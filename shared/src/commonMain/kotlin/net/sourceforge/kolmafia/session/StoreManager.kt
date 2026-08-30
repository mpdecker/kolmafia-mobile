package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.ItemDatabase

/** Headless player-store inventory and purchase-log cache. */
object StoreManager {
    const val MALL_MAX = 999_999_999_999L
    private const val REALISTIC_PRICE_THRESHOLD = 50_000_000L

    enum class TableType { ADDER, PRICER, DEETS }

    data class SoldItem(
        val itemId: Int,
        val quantity: Int,
        val price: Long,
        val limit: Int,
        val lowest: Long = 0,
    )

    data class StoreLogEntry(val id: Int, val text: String) {
        override fun toString(): String = "$id: $text"
    }

    private val soldItems = mutableMapOf<Int, SoldItem>()
    private val storeLog = mutableListOf<StoreLogEntry>()
    var soldItemsRetrieved: Boolean = false
        private set
    var potentialEarnings: Long = 0
        private set

    fun clearCache() {
        soldItems.clear()
        storeLog.clear()
        soldItemsRetrieved = false
        potentialEarnings = 0
    }

    fun getSoldItemList(): List<SoldItem> =
        soldItems.values.sortedWith(compareBy<SoldItem> { it.price == MALL_MAX }.thenBy { it.price })

    fun getStoreLog(): List<StoreLogEntry> = storeLog.toList()
    fun getPrice(itemId: Int): Long = soldItems[itemId]?.price ?: MALL_MAX
    fun getLimit(itemId: Int): Int = soldItems[itemId]?.limit ?: 0
    fun shopAmount(itemId: Int): Int = soldItems[itemId]?.quantity ?: 0

    fun addItem(itemId: Int, quantity: Int, price: Long, limit: Int) {
        if (quantity <= 0) return
        val old = soldItems[itemId]
        soldItems[itemId] = SoldItem(
            itemId,
            (old?.quantity ?: 0) + quantity,
            price.takeIf { it > 0 } ?: MALL_MAX,
            limit,
            old?.lowest ?: 0,
        )
        calculatePotentialEarnings()
    }

    fun updateItem(itemId: Int, quantity: Int, price: Long, limit: Int, lowest: Long = 0) {
        if (quantity <= 0) soldItems.remove(itemId)
        else soldItems[itemId] = SoldItem(itemId, quantity, price, limit, lowest)
        calculatePotentialEarnings()
    }

    fun removeItem(itemId: Int, quantity: Int) {
        val old = soldItems[itemId] ?: return
        val left = (old.quantity - quantity.coerceAtLeast(0)).coerceAtLeast(0)
        if (left == 0) soldItems.remove(itemId) else soldItems[itemId] = old.copy(quantity = left)
        calculatePotentialEarnings()
    }

    fun update(storeText: String, type: TableType) {
        val parsed = when (type) {
            TableType.DEETS -> parseDeets(storeText)
            TableType.PRICER -> parsePricer(storeText)
            TableType.ADDER -> parseAdder(storeText)
        }
        soldItems.clear()
        parsed.forEach { soldItems[it.itemId] = it }
        soldItemsRetrieved = true
        calculatePotentialEarnings()
    }

    private fun parseDeets(text: String): List<SoldItem> =
        DEETS_ROW.findAll(text).mapNotNull { row ->
            val body = row.value
            val itemId = PRICE_INPUT.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            val price = PRICE_INPUT.find(body)?.groupValues?.get(2)?.replace(",", "")?.toLongOrNull() ?: MALL_MAX
            val limit = LIMIT_INPUT.find(body)?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val quantity = QUANTITY_CELL.find(body)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
            SoldItem(itemId, quantity, price, limit)
        }.toList()

    private fun parsePricer(text: String): List<SoldItem> =
        PRICER.findAll(text).mapNotNull {
            val itemId = it.groupValues[4].toIntOrNull() ?: return@mapNotNull null
            SoldItem(
                itemId,
                it.groupValues[2].replace(",", "").toIntOrNull() ?: 0,
                it.groupValues[3].replace(",", "").toLongOrNull() ?: MALL_MAX,
                it.groupValues[5].toIntOrNull() ?: 0,
                it.groupValues[6].replace(",", "").toLongOrNull() ?: 0,
            )
        }.toList()

    private fun parseAdder(text: String): List<SoldItem> =
        ADDER.findAll(text).mapNotNull {
            val itemId = it.groupValues[6].toIntOrNull() ?: return@mapNotNull null
            SoldItem(
                itemId,
                it.groupValues[3].toIntOrNull() ?: 1,
                it.groupValues[4].replace(",", "").toLongOrNull() ?: MALL_MAX,
                it.groupValues[5].replace(",", "").toIntOrNull() ?: 0,
            )
        }.toList()

    fun updateSomePrices(text: String) {
        val json = text.substringAfter("<!-- U:{", "").substringBefore("-->", "")
        if (json.isEmpty()) return
        UPDATE_PRICE.findAll(json).forEach {
            val itemId = it.groupValues[1].toIntOrNull() ?: return@forEach
            val old = soldItems[itemId] ?: return@forEach
            val price = it.groupValues[2].toLongOrNull() ?: return@forEach
            val limit = it.groupValues[3].toIntOrNull() ?: 0
            soldItems[itemId] = old.copy(price = price, limit = limit, lowest = minOf(old.lowest, price))
        }
        calculatePotentialEarnings()
    }

    fun parseLog(logText: String) {
        storeLog.clear()
        val span = LOG_SPAN.find(logText)?.groupValues?.get(1) ?: return
        val entries = span.split(Regex("""(?i)<br\s*/?>""")).filter { it.isNotBlank() }
        entries.forEachIndexed { index, raw ->
            storeLog += StoreLogEntry(entries.size - index, raw.replace(Regex("<[^>]+>"), "").trim())
        }
    }

    fun calculatePotentialEarnings(): Long {
        potentialEarnings = soldItems.values
            .filter { it.price < REALISTIC_PRICE_THRESHOLD }
            .sumOf { it.quantity.toLong() * it.price }
        return potentialEarnings
    }

    fun pricesAtLowest(avoidMinimumPrice: Boolean = false): Map<Int, Pair<Long, Int>> =
        soldItems.values.associate { item ->
            val autosell = ItemDatabase.getById(item.itemId)?.autosellPrice ?: 0
            val minimum = maxOf(100L, autosell.toLong() * 2)
            val desired = maxOf(minimum, item.lowest - item.lowest % 100)
            val price = if (item.price == MALL_MAX && (!avoidMinimumPrice || desired > minimum)) desired else item.price
            item.itemId to (price to item.limit)
        }

    private val DEETS_ROW = Regex("""<tr class="deets".*?</tr>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val PRICE_INPUT = Regex("""name=["']?price\[(\d+)]["']?\s+value=["']?([\d,]+)""", RegexOption.IGNORE_CASE)
    private val LIMIT_INPUT = Regex("""name=["']?limit\[(\d+)]["']?\s+value=["']?(\d*)""", RegexOption.IGNORE_CASE)
    private val QUANTITY_CELL = Regex(""">([\d,]+)<""")
    private val PRICER = Regex(
        """<tr><td><b>(.*?)&nbsp;.*?<td>([\d,]+)</td>.*?["'](\d+)["'] name=price\d+\[(\d+).*?value=["'](\d+)["'].*?<td>([\d,]+)</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val ADDER = Regex(
        """<tr><td><img src.*?></td><td>(.*?)( *\((\d*)\))?</td><td>([\d,]+)</td><td>(.*?)</td><td.*?(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val LOG_SPAN = Regex("""<span class=?["']?small["']?>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val UPDATE_PRICE = Regex(""""([^"]+)":\{"price":(\d+),"lim":(\d+)}""")
}
