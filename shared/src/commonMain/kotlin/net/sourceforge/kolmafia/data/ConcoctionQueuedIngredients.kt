package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase queued*Ingredients — item consumption reserved by craft queue push. */
object ConcoctionQueuedIngredients {

    enum class Bucket {
        FOOD,
        BOOZE,
        SPLEEN,
        POTION,
    }

    private val food = mutableMapOf<Int, Int>()
    private val booze = mutableMapOf<Int, Int>()
    private val spleen = mutableMapOf<Int, Int>()
    private val potion = mutableMapOf<Int, Int>()

    fun trackConsumption(bucket: Bucket, itemId: Int, quantity: Int) {
        if (quantity <= 0) return
        bucketMap(bucket)[itemId] = (bucketMap(bucket)[itemId] ?: 0) + quantity
    }

    fun releaseConsumption(bucket: Bucket, itemId: Int, quantity: Int) {
        if (quantity <= 0) return
        val map = bucketMap(bucket)
        val next = (map[itemId] ?: 0) - quantity
        if (next <= 0) {
            map.remove(itemId)
        } else {
            map[itemId] = next
        }
    }

    /** Negated net consumption — merged into refresh ingredient counts (desktop getNegation). */
    fun creditForRefresh(): Map<Int, Int> {
        val credits = mutableMapOf<Int, Int>()
        for (map in listOf(food, booze, spleen, potion)) {
            for ((itemId, qty) in map) {
                if (qty == 0) continue
                credits[itemId] = (credits[itemId] ?: 0) - qty
            }
        }
        return credits
    }

    fun fromOrganBucket(bucket: ConcoctionOrganAmounts.QueueBucket): Bucket? = when (bucket) {
        ConcoctionOrganAmounts.QueueBucket.FOOD -> Bucket.FOOD
        ConcoctionOrganAmounts.QueueBucket.BOOZE -> Bucket.BOOZE
        ConcoctionOrganAmounts.QueueBucket.SPLEEN -> Bucket.SPLEEN
        ConcoctionOrganAmounts.QueueBucket.POTION,
        ConcoctionOrganAmounts.QueueBucket.CRAFT,
        -> Bucket.POTION
    }

    internal fun resetForTest() {
        food.clear()
        booze.clear()
        spleen.clear()
        potion.clear()
    }

    private fun bucketMap(bucket: Bucket): MutableMap<Int, Int> = when (bucket) {
        Bucket.FOOD -> food
        Bucket.BOOZE -> booze
        Bucket.SPLEEN -> spleen
        Bucket.POTION -> potion
    }
}
