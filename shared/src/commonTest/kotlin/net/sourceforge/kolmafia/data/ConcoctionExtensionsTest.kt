package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConcoctionExtensionsTest {

    @Test
    fun suseCraftable_singleIngredient() {
        val c = ConcoctionData(
            result = "tasty paste",
            resultQuantity = 1,
            methods = setOf("SUSE"),
            ingredients = listOf(ConcoctionIngredient("meat paste", 1)),
        )
        assertTrue(c.isSuseCraftable())
        assertTrue(c.isAutoCraftable())
        assertFalse(c.isStationCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun stationCraftable_requiresTwoIngredients() {
        val c = ConcoctionData(
            result = "hi mein",
            resultQuantity = 1,
            methods = setOf("COOK"),
            ingredients = listOf(
                ConcoctionIngredient("dry noodles", 1),
                ConcoctionIngredient("sweet s-sauce", 1),
            ),
        )
        assertTrue(c.isStationCraftable())
        assertTrue(c.isAutoCraftable())
        assertFalse(c.isSuseCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun manualNotAutoCraftable() {
        val c = ConcoctionData(
            result = "weird item",
            resultQuantity = 1,
            methods = setOf("SUSE", "MANUAL"),
            ingredients = listOf(ConcoctionIngredient("source", 1)),
        )
        assertFalse(c.isSuseCraftable())
        assertFalse(c.isAutoCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun stillShopRow_parsesRowToken() {
        val c = ConcoctionData(
            result = "bottle of Calcutta Emerald",
            resultQuantity = 1,
            methods = setOf("STILL", "ROW267"),
            ingredients = listOf(ConcoctionIngredient("bottle of gin", 1)),
        )
        assertEquals(267, c.stillShopRow())
        assertTrue(c.isStillCraftable())
        assertFalse(c.isAutoCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun coinmasterCraftable_isCreateSupported() {
        val c = ConcoctionData(
            result = "coin shop item",
            resultQuantity = 1,
            methods = setOf("COINMASTER"),
            ingredients = emptyList(),
        )
        assertTrue(c.isCoinmasterCraftable())
        assertFalse(c.isAutoCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun clipArtCraftable_usesPackedParam() {
        val c = ConcoctionData(
            result = "Ur-Donut",
            resultQuantity = 1,
            methods = setOf("CLIPART"),
            ingredients = emptyList(),
            param = 0x010101,
        )
        assertEquals(Triple(1, 1, 1), c.clipArtParams())
        assertTrue(c.isClipArtCraftable())
        assertFalse(c.isAutoCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun rollCraftable_singleIngredient() {
        val c = ConcoctionData(
            result = "flat dough",
            resultQuantity = 1,
            methods = setOf("ROLL"),
            ingredients = listOf(ConcoctionIngredient("wad of dough", 1)),
        )
        assertTrue(c.isRollCraftable())
        assertFalse(c.isAutoCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun terminalCraftable_mapsExtrudeCommand() {
        val c = ConcoctionData(
            result = "browser cookie",
            resultQuantity = 1,
            methods = setOf("TERMINAL"),
            ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
        )
        assertEquals("extrude -f food.ext", c.terminalExtrudeCommand())
        assertTrue(c.isTerminalCraftable())
        assertFalse(c.isAutoCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun terminalCraftable_unknownResult_notSupported() {
        val c = ConcoctionData(
            result = "unknown terminal item",
            resultQuantity = 1,
            methods = setOf("TERMINAL"),
            ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
        )
        assertEquals(null, c.terminalExtrudeCommand())
        assertFalse(c.isTerminalCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun sewerCraftable_gumIngredient() {
        val c = ConcoctionData(
            result = "seal-skull helmet",
            resultQuantity = 1,
            methods = setOf("SEWER"),
            ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
        )
        assertTrue(c.isSewerCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun sewerCraftable_wrongIngredient_notSupported() {
        val c = ConcoctionData(
            result = "seal-skull helmet",
            resultQuantity = 1,
            methods = setOf("SEWER"),
            ingredients = listOf(ConcoctionIngredient("meat paste", 1)),
        )
        assertFalse(c.isSewerCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun vykeaCraftable_instructionsIngredient() {
        val c = ConcoctionData(
            result = "level 1 bookshelf",
            resultQuantity = 1,
            methods = setOf("VYKEA"),
            ingredients = listOf(
                ConcoctionIngredient("VYKEA instructions", 1),
                ConcoctionIngredient("VYKEA plank", 5),
                ConcoctionIngredient("VYKEA plank", 5),
            ),
        )
        assertTrue(c.isVykeaCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun vykeaCraftable_tooFewIngredients_notSupported() {
        val c = ConcoctionData(
            result = "level 1 bookshelf",
            resultQuantity = 1,
            methods = setOf("VYKEA"),
            ingredients = listOf(ConcoctionIngredient("VYKEA instructions", 1)),
        )
        assertFalse(c.isVykeaCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun museCraftable_singleIngredient() {
        val c = ConcoctionData(
            result = "pottery yo-yo",
            resultQuantity = 1,
            methods = setOf("MUSE"),
            ingredients = listOf(ConcoctionIngredient("smoked potsherd", 5)),
        )
        assertTrue(c.isMuseCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun museCraftable_emptyIngredients_notSupported() {
        val c = ConcoctionData(
            result = "pottery yo-yo",
            resultQuantity = 1,
            methods = setOf("MUSE"),
            ingredients = emptyList(),
        )
        assertFalse(c.isMuseCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun phineasCraftable_sealhideHood() {
        val c = ConcoctionData(
            result = "sealhide hood",
            resultQuantity = 1,
            methods = setOf("PHINEAS"),
            ingredients = listOf(ConcoctionIngredient("hellseal brain", 3)),
        )
        assertTrue(c.isPhineasCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun phineasCraftable_emptyIngredients_notSupported() {
        val c = ConcoctionData(
            result = "sealhide hood",
            resultQuantity = 1,
            methods = setOf("PHINEAS"),
            ingredients = emptyList(),
        )
        assertFalse(c.isPhineasCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun combine_notPhineasCraftable() {
        val c = ConcoctionData(
            result = "meat paste",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(
                ConcoctionIngredient("meat", 1),
                ConcoctionIngredient("meat", 1),
            ),
        )
        assertFalse(c.isPhineasCraftable())
    }

    @Test
    fun staffCraftable_teapotTempest() {
        val c = ConcoctionData(
            result = "Staff of the Teapot Tempest",
            resultQuantity = 1,
            methods = setOf("STAFF"),
            ingredients = listOf(ConcoctionIngredient("big stirring stick", 1)),
        )
        assertTrue(c.isStaffCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun staffCraftable_emptyIngredients_notSupported() {
        val c = ConcoctionData(
            result = "Staff of the Teapot Tempest",
            resultQuantity = 1,
            methods = setOf("STAFF"),
            ingredients = emptyList(),
        )
        assertFalse(c.isStaffCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun combine_notStaffCraftable() {
        val c = ConcoctionData(
            result = "meat paste",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(
                ConcoctionIngredient("meat", 1),
                ConcoctionIngredient("meat", 1),
            ),
        )
        assertFalse(c.isStaffCraftable())
    }

    @Test
    fun tinkerCraftable_clockworkWidget() {
        val c = ConcoctionData(
            result = "clockwork widget",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(
                ConcoctionIngredient("flange", 1),
                ConcoctionIngredient("cog", 1),
                ConcoctionIngredient("sprocket", 1),
            ),
        )
        assertTrue(c.isTinkerCraftable())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun tinkerCraftable_wrongIngredientCount_notSupported() {
        val c = ConcoctionData(
            result = "clockwork widget",
            resultQuantity = 1,
            methods = setOf("TINKER"),
            ingredients = listOf(
                ConcoctionIngredient("flange", 1),
                ConcoctionIngredient("cog", 1),
            ),
        )
        assertFalse(c.isTinkerCraftable())
        assertFalse(c.isCreateSupported())
    }

    @Test
    fun combine_notTinkerCraftable() {
        val c = ConcoctionData(
            result = "meat paste",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(
                ConcoctionIngredient("meat", 1),
                ConcoctionIngredient("meat", 1),
                ConcoctionIngredient("meat", 1),
            ),
        )
        assertFalse(c.isTinkerCraftable())
    }

    @Test
    fun sushiCraftable_beefyNigiri() {
        val c = ConcoctionData(
            result = "beefy nigiri",
            resultQuantity = 1,
            methods = setOf("SUSHI"),
            ingredients = listOf(
                ConcoctionIngredient("beefy fish meat", 1),
                ConcoctionIngredient("white rice", 1),
            ),
        )
        assertTrue(c.isSushiCraftable())
        assertTrue(c.isCreateAndConsume())
        assertTrue(c.isCreateSupported())
    }

    @Test
    fun combine_notSushiCraftable() {
        val c = ConcoctionData(
            result = "meat paste",
            resultQuantity = 1,
            methods = setOf("COMBINE"),
            ingredients = listOf(
                ConcoctionIngredient("meat", 1),
                ConcoctionIngredient("meat", 1),
            ),
        )
        assertFalse(c.isSushiCraftable())
        assertFalse(c.isCreateAndConsume())
    }
}
