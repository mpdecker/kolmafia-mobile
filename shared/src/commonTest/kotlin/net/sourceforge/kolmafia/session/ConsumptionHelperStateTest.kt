package net.sourceforge.kolmafia.session

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket

class ConsumptionHelperStateTest {

    @AfterTest
    fun tearDown() {
        ConsumptionHelperState.resetForTest()
    }

    @Test
    fun queueFoodHelper_accumulatesSameItem() {
        ConsumptionHelperState.queueFoodHelper(5459, 2)
        ConsumptionHelperState.queueFoodHelper(5459, 3)
        assertEquals(5459 to 5, ConsumptionHelperState.currentFoodHelper())
    }

    @Test
    fun queueDrinkHelper_replacesDifferentItem() {
        ConsumptionHelperState.queueDrinkHelper(3324, 1)
        ConsumptionHelperState.queueDrinkHelper(3123, 2)
        assertEquals(3123 to 2, ConsumptionHelperState.currentDrinkHelper())
    }

    @Test
    fun captureAndClearHelper_returnsSnapshotAndClears() {
        ConsumptionHelperState.queueFoodHelper(5459, 2)
        assertEquals(5459 to 2, ConsumptionHelperState.captureAndClearHelper(QueueBucket.FOOD))
        assertNull(ConsumptionHelperState.currentFoodHelper())
    }

    @Test
    fun lastUnconsumed_usesConsumedCounters() {
        ConsumptionHelperState.beginIteration(QueueBucket.FOOD, 3)
        assertEquals(3, ConsumptionHelperState.lastUnconsumed(5, QueueBucket.FOOD))
        ConsumptionHelperState.beginIteration(QueueBucket.BOOZE, 2)
        assertEquals(4, ConsumptionHelperState.lastUnconsumed(5, QueueBucket.BOOZE))
    }

    @Test
    fun utensilForEat_onlyReturnsUtensilHelpers() {
        ConsumptionHelperState.queueFoodHelper(5459, 1)
        assertEquals(5459, ConsumptionHelperState.utensilForEat())
        ConsumptionHelperState.resetForTest()
        ConsumptionHelperState.queueFoodHelper(9999, 1)
        assertNull(ConsumptionHelperState.utensilForEat())
    }

    @Test
    fun utensilForDrink_onlyReturnsUtensilHelpers() {
        ConsumptionHelperState.queueDrinkHelper(3324, 1)
        assertEquals(3324, ConsumptionHelperState.utensilForDrink())
        ConsumptionHelperState.resetForTest()
        ConsumptionHelperState.queueDrinkHelper(4893, 1)
        assertEquals(4893, ConsumptionHelperState.utensilForDrink())
    }

    @Test
    fun decrementFoodHelper_clearsWhenExhausted() {
        ConsumptionHelperState.queueFoodHelper(5459, 1)
        ConsumptionHelperState.decrementFoodHelper()
        assertNull(ConsumptionHelperState.currentFoodHelper())
    }
}
