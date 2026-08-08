package net.sourceforge.kolmafia.data

/** Desktop ClanLoungeRequest.parseFloundry — fish stock counts from lounge HTML. */
object FloundryAvailability {

    private val stockByItemName = mutableMapOf<String, Int>()

    /** Desktop FISH_STOCK_PATTERN — also accepts comma-grouped stock like `<br>1,234 carp`. */
    private val FISH_STOCK_PATTERN = Regex(
        """<br>([\d,]+) (carp|cod|trout|bass|hatchetfish|tuna)""",
    )

    fun reset() {
        stockByItemName.clear()
    }

    fun addFromHtml(html: String) {
        for (match in FISH_STOCK_PATTERN.findAll(html)) {
            val fish = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (fish.isEmpty()) continue
            val itemName = FloundryDatabase.itemNameForFish(fish) ?: continue
            val stock = match.groupValues.getOrNull(1)
                ?.replace(",", "")
                ?.toIntOrNull() ?: 0
            stockByItemName[itemName.lowercase()] = stock
        }
    }

    fun isAvailable(name: String): Boolean = stockForItem(name) >= 10

    fun stockForItem(name: String): Int = stockByItemName[name.lowercase()] ?: 0

    fun creatableCount(name: String): Int {
        val stock = stockForItem(name)
        return if (stock >= 10) stock / 10 else 0
    }

    fun isEmpty(): Boolean = stockByItemName.isEmpty()

    internal fun resetForTest() {
        reset()
    }

    internal fun addForTest(name: String, stock: Int) {
        if (FloundryDatabase.isFloundryItem(name)) {
            stockByItemName[name.lowercase()] = stock
        }
    }

    internal fun snapshotForTest(): Map<String, Int> = stockByItemName.toMap()
}
