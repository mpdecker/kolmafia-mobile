package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ConcoctionAvailableIngredientsTest {

    @Test
    fun aggregate_sumsAcrossSources() {
        val merged = ConcoctionAvailableIngredients.aggregate(
            ConcoctionIngredientSources(
                inventory = mapOf(100 to 1),
                closet = mapOf(100 to 2, 200 to 3),
            ),
        )
        assertEquals(3, merged[100])
        assertEquals(3, merged[200])
    }
}
