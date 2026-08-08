package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState

class ConcoctionLimitPoolsTest {

    @AfterTest
    fun tearDown() {
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun fromLiveSession_meatLimitSubtractsQueuedMeatSpent() {
        ConcoctionQueueBudget.meatSpent = 200
        val pools = ConcoctionLimitPools.fromLiveSession(CharacterState(meat = 500))
        assertEquals(300, pools.meatLimit.initial)
    }

    @Test
    fun forTest_meatLimitSubtractsQueuedMeatSpent() {
        val pools = ConcoctionLimitPools.forTest(meatLimit = 500, meatSpent = 200)
        assertEquals(300, pools.meatLimit.initial)
    }

    @Test
    fun forTest_adventuresUsedSubtractsFromAdventureSmithingPool() {
        val pools = ConcoctionLimitPools.forTest(
            adventureSmithingLimit = 15,
            adventuresUsed = 10,
        )
        assertEquals(5, pools.adventureSmithingLimit.initial)
    }

    @Test
    fun forTest_stillsUsedSubtractsFromStillsPool() {
        val pools = ConcoctionLimitPools.forTest(stillsLimit = 3, stillsUsed = 2)
        assertEquals(1, pools.stillsLimit.initial)
    }
}
