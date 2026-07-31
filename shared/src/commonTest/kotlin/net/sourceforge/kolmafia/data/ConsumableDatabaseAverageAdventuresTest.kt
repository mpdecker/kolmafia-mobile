package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsumableDatabaseAverageAdventuresTest {

    private val testFood = ConsumableData(
        name = "test avg food",
        type = ConsumableType.FOOD,
        amount = 2,
        levelReq = 1,
        quality = ConsumableQuality.DECENT,
        advMin = 4,
        advMax = 6,
        muscMin = 0,
        muscMax = 0,
        mystMin = 0,
        mystMax = 0,
        moxieMin = 0,
        moxieMax = 0,
        notes = "",
    )

    private val testDrink = ConsumableData(
        name = "test avg drink",
        type = ConsumableType.DRINK,
        amount = 2,
        levelReq = 1,
        quality = ConsumableQuality.DECENT,
        advMin = 2,
        advMax = 4,
        muscMin = 0,
        muscMax = 0,
        mystMin = 0,
        mystMax = 0,
        moxieMin = 0,
        moxieMax = 0,
        notes = "",
    )

    @BeforeTest
    fun setUp() {
        ConsumableDatabase.resetConsumablesForTest()
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.injectConsumableForTest(testFood)
        ConsumableDatabase.injectConsumableForTest(testDrink)
        ConsumableDatabase.calculateAllAverageAdventures()
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetConsumablesForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun cacheBuild_returnsBaseMidpoint() {
        assertEquals(5.0, ConsumableDatabase.getAverageAdventures("test avg food"))
    }

    @Test
    fun food_lunchAndGourmand_picksBoostedMap() {
        val base = ConsumableDatabase.getAverageAdventures("test avg food")
        val boosted = ConsumableDatabase.getAverageAdventures(
            "test avg food",
            AverageAdventureContext(
                gloriousLunchActive = true,
                hasGourmand = true,
            ),
        )
        assertEquals(5.0, base)
        assertEquals(9.0, boosted)
    }

    @Test
    fun drink_odeAndRowdy_picksBoostedMap() {
        val base = ConsumableDatabase.getAverageAdventures("test avg drink")
        val boosted = ConsumableDatabase.getAverageAdventures(
            "test avg drink",
            AverageAdventureContext(
                odeActive = true,
                hasRowdyDrinker = true,
            ),
        )
        assertEquals(3.0, base)
        assertEquals(7.0, boosted)
    }

    @Test
    fun sushi_lunchEffectIgnored() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "test avg food",
                resultQuantity = 1,
                methods = setOf("SUSHI"),
                ingredients = listOf(
                    ConcoctionIngredient("white rice", 1),
                    ConcoctionIngredient("seaweed", 1),
                ),
            ),
        )
        val withLunch = ConsumableDatabase.getAverageAdventures(
            "test avg food",
            AverageAdventureContext(gloriousLunchActive = true),
        )
        assertEquals(5.0, withLunch)
    }

    @Test
    fun food_milkAddsFive() {
        val withMilk = ConsumableDatabase.getAverageAdventures(
            "test avg food",
            AverageAdventureContext(milkActive = true),
        )
        assertEquals(10.0, withMilk)
    }

    @Test
    fun slowcore_returnsZero() {
        val value = ConsumableDatabase.getAverageAdventures(
            "test avg food",
            AverageAdventureContext(inSlowcore = true),
        )
        assertEquals(0.0, value)
    }

    @Test
    fun showGainsPerUnit_dividesByAmount() {
        val full = ConsumableDatabase.getAverageAdventures("test avg food")
        val perUnit = ConsumableDatabase.getAverageAdventures(
            "test avg food",
            AverageAdventureContext(perUnit = true),
        )
        assertEquals(5.0, full)
        assertEquals(2.5, perUnit)
    }

    @Test
    fun areAdventuresBoosted_sushiAndStillsuitFalse() {
        val sushi = ConcoctionData(
            result = "sushi item",
            resultQuantity = 1,
            methods = setOf("SUSHI"),
            ingredients = listOf(ConcoctionIngredient("rice", 1)),
        )
        val stillsuit = ConcoctionData(
            result = "stillsuit item",
            resultQuantity = 1,
            methods = setOf("STILLSUIT"),
            ingredients = listOf(ConcoctionIngredient("water", 1)),
        )
        val cook = ConcoctionData(
            result = "cooked item",
            resultQuantity = 1,
            methods = setOf("COOK"),
            ingredients = listOf(
                ConcoctionIngredient("a", 1),
                ConcoctionIngredient("b", 1),
            ),
        )
        assertFalse(sushi.areAdventuresBoosted())
        assertFalse(stillsuit.areAdventuresBoosted())
        assertTrue(cook.areAdventuresBoosted())
    }
}

class ConditionalExtraAdventuresTest {

    private val martiniDrink = ConsumableData(
        name = "test martini",
        type = ConsumableType.DRINK,
        amount = 2,
        levelReq = 1,
        quality = ConsumableQuality.GOOD,
        advMin = 5,
        advMax = 7,
        muscMin = 0,
        muscMax = 0,
        mystMin = 0,
        mystMax = 0,
        moxieMin = 0,
        moxieMax = 0,
        notes = "MARTINI",
    )

    private val wineDrink = ConsumableData(
        name = "test wine",
        type = ConsumableType.DRINK,
        amount = 1,
        levelReq = 1,
        quality = ConsumableQuality.GOOD,
        advMin = 4,
        advMax = 6,
        muscMin = 0,
        muscMax = 0,
        mystMin = 0,
        mystMax = 0,
        moxieMin = 0,
        moxieMax = 0,
        notes = "WINE",
    )

    private val pinkyWineDrink = ConsumableData(
        name = "test pinky wine",
        type = ConsumableType.DRINK,
        amount = 1,
        levelReq = 1,
        quality = ConsumableQuality.GOOD,
        advMin = 8,
        advMax = 8,
        muscMin = 0,
        muscMax = 0,
        mystMin = 0,
        mystMax = 0,
        moxieMin = 0,
        moxieMax = 0,
        notes = "WINE",
    )

    private val lasagnaFood = ConsumableData(
        name = "test lasagna",
        type = ConsumableType.FOOD,
        amount = 3,
        levelReq = 1,
        quality = ConsumableQuality.AWESOME,
        advMin = 12,
        advMax = 16,
        muscMin = 0,
        muscMax = 0,
        mystMin = 0,
        mystMax = 0,
        moxieMin = 0,
        moxieMax = 0,
        notes = "LASAGNA",
    )

    @BeforeTest
    fun setUp() {
        ConsumableDatabase.resetConsumablesForTest()
        ConsumableDatabase.injectConsumableForTest(martiniDrink)
        ConsumableDatabase.injectConsumableForTest(wineDrink)
        ConsumableDatabase.injectConsumableForTest(pinkyWineDrink)
        ConsumableDatabase.injectConsumableForTest(lasagnaFood)
        ConsumableDatabase.calculateAllAverageAdventures()
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetConsumablesForTest()
    }

    @Test
    fun martini_tuxedoEquipped_addsTwo() {
        val base = ConsumableDatabase.getAverageAdventures("test martini")
        val withTuxedo = ConsumableDatabase.getAverageAdventures(
            "test martini",
            extraContext = ConditionalExtraAdventureContext(
                equippedItemNames = setOf("tuxedo shirt"),
            ),
        )
        assertEquals(6.0, base)
        assertEquals(8.0, withTuxedo)
    }

    @Test
    fun martini_autoTuxedoAndAvailable_addsTwo() {
        val withAuto = ConsumableDatabase.getAverageAdventures(
            "test martini",
            extraContext = ConditionalExtraAdventureContext(
                autoTuxedo = true,
                itemAvailable = { it == ConditionalExtraAdventureItems.TUXEDO_SHIRT },
                canEquip = { it == ConditionalExtraAdventureItems.TUXEDO_SHIRT },
            ),
        )
        assertEquals(8.0, withAuto)
    }

    @Test
    fun wine_refinedPalate_addsBonus() {
        val base = ConsumableDatabase.getAverageAdventures("test wine")
        val withPalate = ConsumableDatabase.getAverageAdventures(
            "test wine",
            extraContext = ConditionalExtraAdventureContext(
                activeEffectNames = listOf(ConditionalExtraAdventureEffects.REFINED_PALATE),
            ),
        )
        assertEquals(5.0, base)
        assertEquals(6.0, withPalate)
    }

    @Test
    fun wine_pinkyRing_addsBonus() {
        val withPinky = ConsumableDatabase.getAverageAdventures(
            "test pinky wine",
            extraContext = ConditionalExtraAdventureContext(
                equippedItemIds = setOf(ConditionalExtraAdventureItems.MAFIA_PINKY_RING),
            ),
        )
        assertEquals(9.0, withPinky)
    }

    @Test
    fun lasagna_garishNotMonday_addsFive() {
        val base = ConsumableDatabase.getAverageAdventures("test lasagna")
        val withGarish = ConsumableDatabase.getAverageAdventures(
            "test lasagna",
            extraContext = ConditionalExtraAdventureContext(
                activeEffectNames = listOf(ConditionalExtraAdventureEffects.GARISH),
                isMonday = false,
            ),
        )
        assertEquals(14.0, base)
        assertEquals(19.0, withGarish)
    }

    @Test
    fun lasagna_garishOnMonday_noBonus() {
        val withGarishMonday = ConsumableDatabase.getAverageAdventures(
            "test lasagna",
            extraContext = ConditionalExtraAdventureContext(
                activeEffectNames = listOf(ConditionalExtraAdventureEffects.GARISH),
                isMonday = true,
            ),
        )
        assertEquals(14.0, withGarishMonday)
    }

    @Test
    fun untaggedConsumable_extraContextUnchanged() {
        val food = ConsumableData(
            name = "plain food",
            type = ConsumableType.FOOD,
            amount = 2,
            levelReq = 1,
            quality = ConsumableQuality.DECENT,
            advMin = 4,
            advMax = 6,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "",
        )
        ConsumableDatabase.injectConsumableForTest(food)
        ConsumableDatabase.calculateAllAverageAdventures()
        val base = ConsumableDatabase.getAverageAdventures("plain food")
        val withExtra = ConsumableDatabase.getAverageAdventures(
            "plain food",
            extraContext = ConditionalExtraAdventureContext(
                equippedItemNames = setOf("tuxedo shirt"),
                activeEffectNames = listOf(ConditionalExtraAdventureEffects.GARISH),
            ),
        )
        assertEquals(5.0, base)
        assertEquals(5.0, withExtra)
    }

    @Test
    fun holidayCalendar_mondayDetection() {
        assertEquals(true, HolidayCalendar.isMonday("20260727"))
        assertEquals(false, HolidayCalendar.isMonday("20260728"))
    }

    @Test
    fun pizza_pizzaLover_addsFullness() {
        val pizza = ConsumableData(
            name = "test pizza",
            type = ConsumableType.FOOD,
            amount = 4,
            levelReq = 1,
            quality = ConsumableQuality.DECENT,
            advMin = 2,
            advMax = 4,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "PIZZA",
        )
        ConsumableDatabase.injectConsumableForTest(pizza)
        ConsumableDatabase.calculateAllAverageAdventures()
        val base = ConsumableDatabase.getAverageAdventures("test pizza")
        val withSkill = ConsumableDatabase.getAverageAdventures(
            "test pizza",
            extraContext = ConditionalExtraAdventureContext(
                skillNames = setOf(ConditionalExtraAdventureSkills.PIZZA_LOVER),
            ),
        )
        val perUnit = ConsumableDatabase.getAverageAdventures(
            "test pizza",
            context = AverageAdventureContext(perUnit = true),
            extraContext = ConditionalExtraAdventureContext(
                skillNames = setOf(ConditionalExtraAdventureSkills.PIZZA_LOVER),
            ),
        )
        assertEquals(3.0, base)
        assertEquals(7.0, withSkill)
        assertEquals(1.75, perUnit)
    }

    @Test
    fun beans_beanweaver_addsTwo() {
        val beans = ConsumableData(
            name = "test beans",
            type = ConsumableType.FOOD,
            amount = 1,
            levelReq = 2,
            quality = ConsumableQuality.GOOD,
            advMin = 2,
            advMax = 4,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "BEANS",
        )
        ConsumableDatabase.injectConsumableForTest(beans)
        ConsumableDatabase.calculateAllAverageAdventures()
        val base = ConsumableDatabase.getAverageAdventures("test beans")
        val withSkill = ConsumableDatabase.getAverageAdventures(
            "test beans",
            extraContext = ConditionalExtraAdventureContext(
                skillNames = setOf(ConditionalExtraAdventureSkills.BEANWEAVER),
            ),
        )
        assertEquals(3.0, base)
        assertEquals(5.0, withSkill)
    }

    @Test
    fun saucy_saucemavenMyst_addsFive() {
        val saucy = ConsumableData(
            name = "test saucy myst",
            type = ConsumableType.FOOD,
            amount = 5,
            levelReq = 11,
            quality = ConsumableQuality.AWESOME,
            advMin = 19,
            advMax = 22,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "SAUCY",
        )
        ConsumableDatabase.injectConsumableForTest(saucy)
        ConsumableDatabase.calculateAllAverageAdventures()
        val base = ConsumableDatabase.getAverageAdventures("test saucy myst")
        val withSkill = ConsumableDatabase.getAverageAdventures(
            "test saucy myst",
            extraContext = ConditionalExtraAdventureContext(
                skillNames = setOf(ConditionalExtraAdventureSkills.SAUCEMAVEN),
                isMysticalityClass = true,
            ),
        )
        assertEquals(20.5, base)
        assertEquals(25.5, withSkill)
    }

    @Test
    fun saucy_saucemavenNonMyst_addsThree() {
        val saucy = ConsumableData(
            name = "test saucy non-myst",
            type = ConsumableType.FOOD,
            amount = 5,
            levelReq = 11,
            quality = ConsumableQuality.AWESOME,
            advMin = 19,
            advMax = 22,
            muscMin = 0,
            muscMax = 0,
            mystMin = 0,
            mystMax = 0,
            moxieMin = 0,
            moxieMax = 0,
            notes = "SAUCY",
        )
        ConsumableDatabase.injectConsumableForTest(saucy)
        ConsumableDatabase.calculateAllAverageAdventures()
        val withSkill = ConsumableDatabase.getAverageAdventures(
            "test saucy non-myst",
            extraContext = ConditionalExtraAdventureContext(
                skillNames = setOf(ConditionalExtraAdventureSkills.SAUCEMAVEN),
                isMysticalityClass = false,
            ),
        )
        assertEquals(23.5, withSkill)
    }

    @Test
    fun bondcore_martiniGif_compositeBonus() {
        val withBond = ConsumableDatabase.getAverageAdventures(
            "test martini",
            extraContext = ConditionalExtraAdventureContext(
                inBondcore = true,
                bondMartiniTurn = true,
                bondMartiniPlus = true,
                equippedItemNames = setOf("tuxedo shirt"),
                itemImage = { "martini.gif" },
            ),
        )
        assertEquals(13.0, withBond)
    }

    @Test
    fun bondcore_nonMartiniGif_fallsThroughToV1Martini() {
        val withBondOtherImage = ConsumableDatabase.getAverageAdventures(
            "test martini",
            extraContext = ConditionalExtraAdventureContext(
                inBondcore = true,
                bondMartiniTurn = true,
                bondMartiniPlus = true,
                equippedItemNames = setOf("tuxedo shirt"),
                itemImage = { "other.gif" },
            ),
        )
        assertEquals(8.0, withBondOtherImage)
    }
}
