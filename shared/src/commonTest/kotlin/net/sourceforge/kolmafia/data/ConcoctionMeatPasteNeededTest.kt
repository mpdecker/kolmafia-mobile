package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState

class ConcoctionMeatPasteNeededTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun getMeatPasteNeeded_flatCombine_returnsQuantityNeeded() {
        val concoction = combineConcoction("flat combine", listOf(ConcoctionIngredient("part a", 1)))

        assertEquals(3, ConcoctionMeatPasteNeeded.getMeatPasteNeeded(concoction, quantityNeeded = 3))
    }

    @Test
    fun getMeatPasteNeeded_nestedCombine_sumsChildren() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "child part",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("leaf", 1)),
            ),
        )
        val parent = combineConcoction(
            "parent item",
            listOf(ConcoctionIngredient("child part", 1)),
        )

        assertEquals(4, ConcoctionMeatPasteNeeded.getMeatPasteNeeded(parent, quantityNeeded = 2))
    }

    @Test
    fun getMeatPasteNeeded_jewelry_included() {
        val concoction = ConcoctionData(
            result = "jewelry item",
            resultQuantity = 1,
            methods = setOf("JEWELRY"),
            ingredients = listOf(ConcoctionIngredient("gem", 1)),
        )

        assertEquals(2, ConcoctionMeatPasteNeeded.getMeatPasteNeeded(concoction, quantityNeeded = 2))
    }

    @Test
    fun getMeatPasteNeeded_knollAvailableAndNotZombie_returnsZero() {
        val concoction = combineConcoction("flat combine", listOf(ConcoctionIngredient("part a", 1)))
        val state = CharacterState(zodiacSign = "Mongoose")

        assertEquals(0, ConcoctionMeatPasteNeeded.getMeatPasteNeeded(concoction, quantityNeeded = 3, state = state))
        assertFalse(ConcoctionMeatPasteNeeded.needsPaste(concoction, state))
    }

    @Test
    fun needsPaste_zombiecoreEvenWithKnoll_returnsTrue() {
        val concoction = combineConcoction("flat combine", listOf(ConcoctionIngredient("part a", 1)))
        val state = CharacterState(
            zodiacSign = "Mongoose",
            challengePath = "Zombie Slayer",
        )

        assertTrue(ConcoctionMeatPasteNeeded.needsPaste(concoction, state))
        assertEquals(2, ConcoctionMeatPasteNeeded.getMeatPasteNeeded(concoction, quantityNeeded = 2, state = state))
    }

    private fun combineConcoction(
        result: String,
        ingredients: List<ConcoctionIngredient>,
    ): ConcoctionData = ConcoctionData(
        result = result,
        resultQuantity = 1,
        methods = setOf("COMBINE"),
        ingredients = ingredients,
    )
}
