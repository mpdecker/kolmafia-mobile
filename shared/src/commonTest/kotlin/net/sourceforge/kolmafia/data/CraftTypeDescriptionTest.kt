package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class CraftTypeDescriptionTest {

    @Test
    fun describe_combine() {
        assertEquals("Meatpasting", CraftTypeDescription.describe(setOf("COMBINE")))
    }

    @Test
    fun describe_still() {
        assertEquals("Nash Crosby's Still", CraftTypeDescription.describe(setOf("STILL")))
    }

    @Test
    fun describe_jewel() {
        assertEquals("Jewelry-making pliers", CraftTypeDescription.describe(setOf("JEWEL")))
    }

    @Test
    fun describe_cookPastamastery() {
        assertEquals(
            "Cooking (Pastamastery)",
            CraftTypeDescription.describe(setOf("COOK", "PASTAMASTERY")),
        )
    }

    @Test
    fun describe_acock() {
        assertEquals(
            "Mixing (fancy) (Advanced Cocktailcrafting)",
            CraftTypeDescription.describe(setOf("ACOCK")),
        )
    }

    @Test
    fun describe_unknownWhenEmpty() {
        assertEquals("[unknown method of creation]", CraftTypeDescription.describe(emptySet()))
    }

    @Test
    fun describe_stillIgnoresRowToken() {
        assertEquals(
            "Nash Crosby's Still",
            CraftTypeDescription.describe(setOf("STILL", "ROW267")),
        )
    }

    @Test
    fun concoctionDataExtension() {
        val c = ConcoctionData(
            result = "bottle of gin",
            resultQuantity = 1,
            methods = setOf("STILL"),
            ingredients = emptyList(),
        )
        assertEquals("Nash Crosby's Still", c.craftTypeDescription())
    }
}
