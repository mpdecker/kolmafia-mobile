package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState

class ConcoctionLimitPoolVisibleTotalTest {

    @AfterTest
    fun tearDown() {
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun fromLiveSession_exposesGrossAndNet() {
        ConcoctionQueueBudget.adventuresUsed = 3
        ConcoctionQueueBudget.stillsUsed = 2

        val state = CharacterState(
            challengePath = "Standard",
            adventuresLeft = 10,
            stillsAvailable = 5,
            meat = 1000,
        )
        val pools = ConcoctionLimitPools.fromLiveSession(state)

        assertEquals(10, pools.adventureLimit.visibleTotal)
        assertEquals(7, pools.adventureLimit.initial)
        assertTrue(pools.adventureLimit.visibleTotal > pools.adventureLimit.initial)

        assertEquals(5, pools.stillsLimit.visibleTotal)
        assertEquals(3, pools.stillsLimit.initial)
    }

    @Test
    fun forTest_exposesGrossAndNet() {
        val pools = ConcoctionLimitPools.forTest(
            adventureLimit = 20,
            adventuresUsed = 5,
        )

        assertEquals(20, pools.adventureLimit.visibleTotal)
        assertEquals(15, pools.adventureLimit.initial)
    }
}
