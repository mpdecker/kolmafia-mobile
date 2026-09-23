package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionYield
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

/** MANUAL concoction edges residual mega wrap (phases 7511–7570). */
class GameRuntimeLibraryPhase7570Test {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_isPhase7570() {
        assertEquals("phase7630", GameRuntimeLibrary.REVISION)
        assertEquals("7630", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun recipeCli_printsYieldAndNestedIngredients() {
        ItemDatabase.registerForTest(
            ItemData(9201, "nest toast", "", "", ItemPrimaryUse.FOOD, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(9202, "nest bread", "", "", ItemPrimaryUse.FOOD, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(9203, "nest flour", "", "", ItemPrimaryUse.FOOD, emptySet(), emptySet(), 0, null),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nest bread",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(ConcoctionIngredient("nest flour", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nest toast",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(ConcoctionIngredient("nest bread", 1)),
            ),
        )
        val lib = GameRuntimeLibrary.forTesting()
        val recipe = outputLib(lib, """cli_execute("recipe nest toast");""")
        assertTrue(recipe.contains("nest toast"), recipe)
        assertTrue(recipe.contains("nest bread"), recipe)
        val ingredients = outputLib(lib, """cli_execute("ingredients nest toast");""")
        assertTrue(ingredients.contains("nest flour"), ingredients)
    }

    @Test
    fun yieldHelper_isLive() {
        val potion = ConcoctionData(
            result = "oil of expertise",
            resultQuantity = 1,
            methods = setOf("SAUCE", "SX3"),
            ingredients = emptyList(),
        )
        assertEquals(3, ConcoctionYield.getYield(potion, tripleReagent = true))
    }
}
