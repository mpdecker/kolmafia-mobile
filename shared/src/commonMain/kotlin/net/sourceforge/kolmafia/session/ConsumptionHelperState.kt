package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket

/** Desktop EatItemRequest/DrinkItemRequest helper slots + foodConsumed/boozeConsumed counters. */
object ConsumptionHelperState {
    private const val SCRATCHS_FORK = 3323
    private const val FUDGE_SPORK = 5459
    private const val FROSTYS_MUG = 3324
    private const val DIVINE_FLUTE = 3123
    private const val CRIMBCO_MUG = 4880
    private const val BGE_SHOTGLASS = 4893

    private var foodHelperItemId: Int? = null
    private var foodHelperCount: Int = 0
    private var drinkHelperItemId: Int? = null
    private var drinkHelperCount: Int = 0

    var foodConsumed: Int = 0
        private set
    var boozeConsumed: Int = 0
        private set

    fun resetConsumedCounters() {
        foodConsumed = 0
        boozeConsumed = 0
    }

    fun beginIteration(bucket: QueueBucket, iteration: Int) {
        when (bucket) {
            QueueBucket.FOOD -> foodConsumed = iteration - 1
            QueueBucket.BOOZE -> boozeConsumed = iteration - 1
            else -> Unit
        }
    }

    fun markFullyConsumed(bucket: QueueBucket, quantity: Int) {
        when (bucket) {
            QueueBucket.FOOD -> foodConsumed = quantity
            QueueBucket.BOOZE -> boozeConsumed = quantity
            else -> Unit
        }
    }

    fun queueFoodHelper(itemId: Int, count: Int) {
        if (count <= 0) return
        if (foodHelperItemId == itemId) {
            foodHelperCount += count
        } else {
            foodHelperItemId = itemId
            foodHelperCount = count
        }
    }

    fun queueDrinkHelper(itemId: Int, count: Int) {
        if (count <= 0) return
        if (drinkHelperItemId == itemId) {
            drinkHelperCount += count
        } else {
            drinkHelperItemId = itemId
            drinkHelperCount = count
        }
    }

    fun currentFoodHelper(): Pair<Int, Int>? {
        val id = foodHelperItemId ?: return null
        if (foodHelperCount <= 0) return null
        return id to foodHelperCount
    }

    fun currentDrinkHelper(): Pair<Int, Int>? {
        val id = drinkHelperItemId ?: return null
        if (drinkHelperCount <= 0) return null
        return id to drinkHelperCount
    }

    fun captureAndClearHelper(bucket: QueueBucket): Pair<Int, Int>? {
        val captured = when (bucket) {
            QueueBucket.FOOD -> currentFoodHelper()
            QueueBucket.BOOZE -> currentDrinkHelper()
            else -> null
        }
        when (bucket) {
            QueueBucket.FOOD -> clearFoodHelper()
            QueueBucket.BOOZE -> clearDrinkHelper()
            else -> Unit
        }
        return captured
    }

    fun lastUnconsumed(quantity: Int, bucket: QueueBucket): Int =
        quantity - when (bucket) {
            QueueBucket.FOOD -> foodConsumed
            QueueBucket.BOOZE -> boozeConsumed
            else -> 0
        }

    fun utensilForEat(): Int? {
        val id = foodHelperItemId ?: return null
        if (foodHelperCount <= 0) return null
        return when (id) {
            SCRATCHS_FORK, FUDGE_SPORK -> id
            else -> null
        }
    }

    fun utensilForDrink(): Int? {
        val id = drinkHelperItemId ?: return null
        if (drinkHelperCount <= 0) return null
        return when (id) {
            FROSTYS_MUG, DIVINE_FLUTE, CRIMBCO_MUG, BGE_SHOTGLASS -> id
            else -> null
        }
    }

    fun decrementFoodHelper() {
        if (foodHelperCount > 0) {
            foodHelperCount -= 1
            if (foodHelperCount <= 0) {
                clearFoodHelper()
            }
        }
    }

    fun decrementDrinkHelper() {
        if (drinkHelperCount > 0) {
            drinkHelperCount -= 1
            if (drinkHelperCount <= 0) {
                clearDrinkHelper()
            }
        }
    }

    /** Desktop EatItemRequest.clearFoodHelper — ASH `clear_food_helper`. */
    fun clearFoodHelper() {
        foodHelperItemId = null
        foodHelperCount = 0
    }

    /** Desktop DrinkItemRequest.clearBoozeHelper — ASH `clear_booze_helper`. */
    fun clearDrinkHelper() {
        drinkHelperItemId = null
        drinkHelperCount = 0
    }

    /** Alias for ASH `clear_booze_helper`. */
    fun clearBoozeHelper() = clearDrinkHelper()

    internal fun resetForTest() {
        clearFoodHelper()
        clearDrinkHelper()
        resetConsumedCounters()
    }
}
