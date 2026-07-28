package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ConcoctionCreationCostTest {

    @Test
    fun creationCost_combineIsTen() {
        assertEquals(10L, ConcoctionCreationCost.creationCost(setOf("COMBINE")))
    }

    @Test
    fun adventureUsage_smithIsOne() {
        assertEquals(1, ConcoctionCreationCost.adventureUsage(setOf("SMITH", "HAMMER")))
    }

    @Test
    fun primaryMethod_prefersCombineOverRequirements() {
        assertEquals("COMBINE", ConcoctionCreationCost.primaryMethod(setOf("COMBINE", "MALE")))
    }
}
