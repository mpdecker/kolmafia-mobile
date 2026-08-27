package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.platform.UserDataFileIO

/** Local `mallprices.txt` history shared by historical_price() and mall_price(maxAge). */
object MallPriceDatabase {
    private const val VERSION = 0xF00D5
    private const val FILE = "mallprices.txt"
    private const val MALL_MAX = 999_999_999_999L

    data class Price(val itemId: Int, val price: Long, val timestampSeconds: Long)

    private val prices = sortedMapOf<Int, Price>()
    private var loaded = false

    fun load(text: String? = UserDataFileIO.readText(FILE)): Int {
        loaded = true
        if (text == null) return 0
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.firstOrNull()?.trim()?.toIntOrNull() != VERSION) return 0
        val now = currentEpochSeconds()
        var count = 0
        for (line in lines.drop(1)) {
            val fields = line.split('\t')
            val id = fields.getOrNull(0)?.toIntOrNull() ?: continue
            val timestamp = fields.getOrNull(1)?.toLongOrNull()?.coerceAtMost(now) ?: continue
            val price = fields.getOrNull(2)?.toLongOrNull() ?: continue
            if (id <= 0 || timestamp <= 0 || price !in 1..MALL_MAX) continue
            val old = prices[id]
            if (old == null || timestamp > old.timestampSeconds) {
                prices[id] = Price(id, price, timestamp)
            }
            count++
        }
        return count
    }

    private fun ensureLoaded() {
        if (!loaded) load()
    }

    fun recordPrice(itemId: Int, price: Long, timestampSeconds: Long = currentEpochSeconds(), deferred: Boolean = false) {
        if (itemId <= 0 || price !in 1..MALL_MAX) return
        ensureLoaded()
        prices[itemId] = Price(itemId, price, timestampSeconds)
        if (!deferred) save()
    }

    fun getPrice(itemId: Int): Long {
        ensureLoaded()
        return prices[itemId]?.price ?: 0
    }

    fun getAgeSeconds(itemId: Int, nowSeconds: Long = currentEpochSeconds()): Long? {
        ensureLoaded()
        return prices[itemId]?.let { (nowSeconds - it.timestampSeconds).coerceAtLeast(0) }
    }

    fun getAgeDays(itemId: Int): Double =
        getAgeSeconds(itemId)?.div(86_400.0) ?: Double.POSITIVE_INFINITY

    fun save() {
        ensureLoaded()
        val text = buildString {
            append(VERSION).append('\n')
            prices.values.forEach {
                append(it.itemId).append('\t').append(it.timestampSeconds).append('\t').append(it.price).append('\n')
            }
        }
        UserDataFileIO.writeText(FILE, text)
    }

    fun resetForTest() {
        prices.clear()
        loaded = true
    }
}
