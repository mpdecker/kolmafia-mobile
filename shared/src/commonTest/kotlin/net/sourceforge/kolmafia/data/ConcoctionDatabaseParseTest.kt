package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcoctionDatabaseParseTest {

    @AfterTest
    fun cleanup() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun parse_clipArtLine_packsParamAndSkipsNumericIngredients() {
        ConcoctionDatabase.parseForTest("Ur-Donut\tCLIPART\t1\t1\t1")

        val concoction = ConcoctionDatabase.getByResult("Ur-Donut")
        assertTrue(concoction != null)
        assertEquals(setOf("CLIPART"), concoction.methods)
        assertEquals(0x010101, concoction.param)
        assertTrue(concoction.ingredients.isEmpty())
        assertEquals(Triple(1, 1, 1), concoction.clipArtParams())
    }

    @Test
    fun parse_rollLine_keepsIngredientNames() {
        ConcoctionDatabase.parseForTest("flat dough\tROLL\twad of dough")

        val concoction = ConcoctionDatabase.getByResult("flat dough")
        assertTrue(concoction != null)
        assertEquals(0, concoction.param)
        assertEquals(listOf(ConcoctionIngredient("wad of dough", 1)), concoction.ingredients)
    }
}
