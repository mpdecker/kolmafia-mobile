package net.sourceforge.kolmafia.maximizer

/**
 * Desktop [CheckedItem] acquisition subset for ranked maximizer buckets.
 * Phase 365: initial/creatable/fold/pull/buy channels summed by [totalCount].
 */
data class MaximizerCheckedItem(
    val itemId: Int,
    val name: String,
    val initial: Int = 0,
    val creatable: Int = 0,
    val npcBuyable: Int = 0,
    val mallBuyable: Int = 0,
    val foldable: Int = 0,
    val pullable: Int = 0,
    val pullfoldable: Int = 0,
    val pullBuyable: Int = 0,
    val foldItemId: Int = 0,
    val buyableFlag: Boolean = false,
) {
    /** Desktop CheckedItem.getCount() — sum of all acquisition channels. */
    fun totalCount(): Int =
        initial +
            creatable +
            npcBuyable +
            mallBuyable +
            foldable +
            pullable +
            pullfoldable +
            pullBuyable

    /** Desktop CheckedItem.validate — post-prefetch mall price + meat budget gates. */
    fun validate(
        maxPrice: Long?,
        priceLevel: MaximizerPriceLevel,
        availableMeat: Long,
        storageMeat: Long,
        mallPrice: (Int) -> Long,
    ): MaximizerCheckedItem {
        if (priceLevel == MaximizerPriceLevel.DONT_CHECK || !buyableFlag) return this
        val price = mallPrice(itemId)
        var newMallBuyable = mallBuyable
        var newPullBuyable = pullBuyable
        if (price <= 0L || (maxPrice != null && price > maxPrice)) {
            newMallBuyable = 0
            newPullBuyable = 0
        }
        if (price > availableMeat) newMallBuyable = 0
        if (price > storageMeat) newPullBuyable = 0
        return copy(mallBuyable = newMallBuyable, pullBuyable = newPullBuyable)
    }

    /**
     * Desktop Maximizer emit-time mall re-check for [MaximizerPriceLevel.ALL] when non-mall
     * acquisition channels exist (initial/creatable/pull/NPC).
     */
    fun passesEmitMallCheck(
        priceLevel: MaximizerPriceLevel,
        maxPrice: Long?,
        mallPrice: Long,
        historicalPrice: Long,
        tradeable: Boolean,
    ): Boolean {
        if (priceLevel != MaximizerPriceLevel.ALL || maxPrice == null) return true
        val hasNonMallChannel = initial > 0 || creatable > 0 || pullable > 0 || npcBuyable > 0
        if (!hasNonMallChannel) return true
        if (!tradeable) return true
        if (historicalPrice > maxPrice * 2) return false
        if (mallPrice <= 0L) return true
        return mallPrice <= maxPrice
    }
}

