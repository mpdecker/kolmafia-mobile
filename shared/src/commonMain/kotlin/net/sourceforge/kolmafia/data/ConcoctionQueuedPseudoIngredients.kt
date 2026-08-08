package net.sourceforge.kolmafia.data

/** Desktop queued*Ingredients pseudo rows (ADV/MEAT/PULL/TOME/STILL/EXTRUDE/FREE_CRAFT) — bucket display parity. */
object ConcoctionQueuedPseudoIngredients {

    enum class PseudoType {
        ADV,
        MEAT,
        PULL,
        TOME,
        STILL,
        EXTRUDE,
        FREE_CRAFT,
    }

    private val food = mutableMapOf<PseudoType, Int>()
    private val booze = mutableMapOf<PseudoType, Int>()
    private val spleen = mutableMapOf<PseudoType, Int>()
    private val potion = mutableMapOf<PseudoType, Int>()

    fun track(bucket: ConcoctionQueuedIngredients.Bucket, delta: PseudoIngredientDelta) {
        if (delta.adventures != 0) add(bucket, PseudoType.ADV, delta.adventures)
        if (delta.meat != 0) add(bucket, PseudoType.MEAT, delta.meat)
        if (delta.pulls != 0) add(bucket, PseudoType.PULL, delta.pulls)
        if (delta.tomes != 0) add(bucket, PseudoType.TOME, delta.tomes)
        if (delta.stills != 0) add(bucket, PseudoType.STILL, delta.stills)
        if (delta.extrudes != 0) add(bucket, PseudoType.EXTRUDE, delta.extrudes)
        if (delta.freeCrafts != 0) add(bucket, PseudoType.FREE_CRAFT, delta.freeCrafts)
    }

    fun release(bucket: ConcoctionQueuedIngredients.Bucket, delta: PseudoIngredientDelta) {
        if (delta.adventures != 0) subtract(bucket, PseudoType.ADV, delta.adventures)
        if (delta.meat != 0) subtract(bucket, PseudoType.MEAT, delta.meat)
        if (delta.pulls != 0) subtract(bucket, PseudoType.PULL, delta.pulls)
        if (delta.tomes != 0) subtract(bucket, PseudoType.TOME, delta.tomes)
        if (delta.stills != 0) subtract(bucket, PseudoType.STILL, delta.stills)
        if (delta.extrudes != 0) subtract(bucket, PseudoType.EXTRUDE, delta.extrudes)
        if (delta.freeCrafts != 0) subtract(bucket, PseudoType.FREE_CRAFT, delta.freeCrafts)
    }

    fun count(bucket: ConcoctionQueuedIngredients.Bucket, type: PseudoType): Int =
        bucketMap(bucket)[type] ?: 0

    /** Sum pseudo rows across all organ buckets (desktop queued*Ingredients aggregate). */
    fun totalsByType(): Map<PseudoType, Int> {
        val totals = mutableMapOf<PseudoType, Int>()
        for (map in listOf(food, booze, spleen, potion)) {
            for ((type, qty) in map) {
                if (qty == 0) continue
                totals[type] = (totals[type] ?: 0) + qty
            }
        }
        return totals
    }

    fun totalAdventures(): Int = totalsByType()[PseudoType.ADV] ?: 0
    fun totalFreeCrafts(): Int = totalsByType()[PseudoType.FREE_CRAFT] ?: 0
    fun totalMeat(): Int = totalsByType()[PseudoType.MEAT] ?: 0
    fun totalPulls(): Int = totalsByType()[PseudoType.PULL] ?: 0
    fun totalTomes(): Int = totalsByType()[PseudoType.TOME] ?: 0
    fun totalStills(): Int = totalsByType()[PseudoType.STILL] ?: 0
    fun totalExtrudes(): Int = totalsByType()[PseudoType.EXTRUDE] ?: 0

    /** Verify bucket pseudo totals align with [ConcoctionQueueBudget] craft counters. */
    fun reconcileWithBudget(): Boolean {
        val totals = totalsByType()
        val budget = ConcoctionQueueBudget
        val advQueued = totals[PseudoType.ADV] ?: 0
        val freeQueued = totals[PseudoType.FREE_CRAFT] ?: 0
        return advQueued + freeQueued == budget.adventuresUsed + budget.freeCraftingTurns &&
            (totals[PseudoType.MEAT] ?: 0) == budget.meatSpent &&
            (totals[PseudoType.PULL] ?: 0) == budget.pullsUsed &&
            (totals[PseudoType.TOME] ?: 0) == budget.tomesUsed &&
            (totals[PseudoType.STILL] ?: 0) == budget.stillsUsed &&
            (totals[PseudoType.EXTRUDE] ?: 0) == budget.extrudesUsed
    }

    internal fun resetForTest() {
        food.clear()
        booze.clear()
        spleen.clear()
        potion.clear()
    }

    private fun add(bucket: ConcoctionQueuedIngredients.Bucket, type: PseudoType, quantity: Int) {
        val map = bucketMap(bucket)
        map[type] = (map[type] ?: 0) + quantity
    }

    private fun subtract(bucket: ConcoctionQueuedIngredients.Bucket, type: PseudoType, quantity: Int) {
        val map = bucketMap(bucket)
        val next = (map[type] ?: 0) - quantity
        if (next <= 0) {
            map.remove(type)
        } else {
            map[type] = next
        }
    }

    private fun bucketMap(bucket: ConcoctionQueuedIngredients.Bucket): MutableMap<PseudoType, Int> =
        when (bucket) {
            ConcoctionQueuedIngredients.Bucket.FOOD -> food
            ConcoctionQueuedIngredients.Bucket.BOOZE -> booze
            ConcoctionQueuedIngredients.Bucket.SPLEEN -> spleen
            ConcoctionQueuedIngredients.Bucket.POTION -> potion
        }
}

data class PseudoIngredientDelta(
    val adventures: Int = 0,
    val meat: Int = 0,
    val pulls: Int = 0,
    val tomes: Int = 0,
    val stills: Int = 0,
    val extrudes: Int = 0,
    val freeCrafts: Int = 0,
)
