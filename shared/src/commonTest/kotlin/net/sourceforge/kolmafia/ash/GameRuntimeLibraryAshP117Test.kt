package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class GameRuntimeLibraryAshP117Test {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun craftType_returnsDescriptionForKnownRecipe() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "meat paste",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("meat", 1)),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals(
            "Meatpasting",
            outputLib(lib, """print(craft_type(to_item("meat paste")));""").trim(),
        )
    }

    @Test
    fun craftType_returnsNoneForUnknownItem() {
        val lib = GameRuntimeLibrary()
        assertEquals("none", outputLib(lib, """print(craft_type(to_item("seal tooth")));""").trim())
    }

    @Test
    fun craftType_byItemId() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 752,
                name = "bottle of gin",
                descId = "",
                image = "",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "bottle of gin",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = listOf(ConcoctionIngredient("bottle of rum", 1)),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("Nash Crosby's Still", outputLib(lib, """print(craft_type(752));""").trim())
    }

    @Test
    fun craftType_resolvesItemAliasViaGameDatabase() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "hi mein",
                resultQuantity = 1,
                methods = setOf("COOK", "PASTAMASTERY"),
                ingredients = listOf(
                    ConcoctionIngredient("dry noodles", 1),
                    ConcoctionIngredient("sweet s-sauce", 1),
                ),
            ),
        )
        val db = object : GameDatabase() {
            override fun item(name: String) = ItemData(
                id = 999,
                name = "hi mein",
                descId = "",
                image = "",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Cooking (Pastamastery)",
            outputLib(lib, """print(craft_type(to_item("Hi Mein")));""").trim(),
        )
    }

    @Test
    fun revision_phase160() {
        assertEquals("phase460", GameRuntimeLibrary.REVISION)
    }
}
