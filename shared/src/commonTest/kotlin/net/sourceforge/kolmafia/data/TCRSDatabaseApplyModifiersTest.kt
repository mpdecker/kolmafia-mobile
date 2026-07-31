package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.TCRSDatabase.TcrsEntry
import net.sourceforge.kolmafia.preferences.Preferences

class TCRSDatabaseApplyModifiersTest {

    private val testItemId = 9_000_001
    private val familiarItemId = 9_000_002
    private val foodItemId = 9_000_003
    private val spleenItemId = 9_000_004
    private val effectFoodItemId = 9_000_005
    private val itemName = "tcrs-test-item"
    private val foodName = "tcrs-test-food"
    private val spleenName = "tcrs-test-spleen"
    private val effectFoodName = "tcrs-effect-food"
    private val effectName = "Tcrs Foo Effect"

    @BeforeTest
    fun setUp() {
        ItemDatabase.registerForTest(
            ItemData(
                id = testItemId,
                name = itemName,
                descId = "tcrs-test",
                image = "test.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = familiarItemId,
                name = "tcrs-fam-item",
                descId = "tcrs-fam",
                image = "fam.gif",
                primaryUse = ItemPrimaryUse.FAMILIAR,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = foodItemId,
                name = foodName,
                descId = "tcrs-food",
                image = "food.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = spleenItemId,
                name = spleenName,
                descId = "tcrs-spleen",
                image = "spleen.gif",
                primaryUse = ItemPrimaryUse.SPLEEN,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = TCRSSkipItemIds.CAMPGROUND_ITEMS.first(),
                name = "campground-skip-item",
                descId = "camp",
                image = "camp.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = TCRSSkipItemIds.CHATEAU_ITEMS.first(),
                name = "chateau-skip-item",
                descId = "chateau",
                image = "chateau.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", itemName, "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "tcrs-fam-item", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "campground-skip-item", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "chateau-skip-item", "Meat Drop: +1")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = foodName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 2,
                advMax = 3,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "base",
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = spleenName,
                type = ConsumableType.SPLEEN,
                amount = 1,
                levelReq = 5,
                quality = ConsumableQuality.GOOD,
                advMin = 0,
                advMax = 0,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "base",
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = effectFoodItemId,
                name = effectFoodName,
                descId = "tcrs-effect-food",
                image = "effectfood.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = effectFoodName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 2,
                advMax = 3,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "base",
            ),
        )
        ModifierDatabase.injectForTest("Item", effectFoodName, "Meat Drop: +1")
        EffectDatabase.registerForTest(
            EffectData(
                id = 9_000_010,
                name = effectName,
                image = "foo.gif",
                descId = "foo",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 old food",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = effectFoodName,
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(ConcoctionIngredient("ingredient", 1)),
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
        CafeDatabase.resetForTest()
        ModifierDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        EffectDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun applyModifiers_overridesItemModifiers() {
        TCRSDatabase.injectMapForTest(
            mapOf(testItemId to TcrsEntry(name = "TCRS Sword", modifiers = "Meat Drop: +10")),
        )
        val applied = TCRSDatabase.applyModifiers(11)
        assertEquals(1, applied)
        assertTrue(ModifierDatabase.getItem(itemName)?.modifiers?.contains("Meat Drop: +10") == true)
    }

    @Test
    fun applyModifiers_skipsFamiliarEquipment() {
        TCRSDatabase.injectMapForTest(
            mapOf(familiarItemId to TcrsEntry(name = "TCRS Fam", modifiers = "Meat Drop: +10")),
        )
        val applied = TCRSDatabase.applyModifiers(11)
        assertEquals(0, applied)
        assertEquals("Meat Drop: +1", ModifierDatabase.getItem("tcrs-fam-item")?.modifiers)
    }

    @Test
    fun applyModifiers_skipsCampgroundItem() {
        val campgroundId = TCRSSkipItemIds.CAMPGROUND_ITEMS.first()
        TCRSDatabase.injectMapForTest(
            mapOf(campgroundId to TcrsEntry(name = "TCRS Camp", modifiers = "Meat Drop: +10")),
        )
        val applied = TCRSDatabase.applyModifiers(11)
        assertEquals(0, applied)
        assertEquals("Meat Drop: +1", ModifierDatabase.getItem("campground-skip-item")?.modifiers)
    }

    @Test
    fun applyModifiers_skipsChateauItem() {
        val chateauId = TCRSSkipItemIds.CHATEAU_ITEMS.first()
        TCRSDatabase.injectMapForTest(
            mapOf(chateauId to TcrsEntry(name = "TCRS Chateau", modifiers = "Meat Drop: +10")),
        )
        val applied = TCRSDatabase.applyModifiers(11)
        assertEquals(0, applied)
        assertEquals("Meat Drop: +1", ModifierDatabase.getItem("chateau-skip-item")?.modifiers)
    }

    @Test
    fun applyModifiers_blankModifiersStillUpdatesItem() {
        TCRSDatabase.injectMapForTest(
            mapOf(testItemId to TcrsEntry(name = "TCRS Sword", modifiers = "")),
        )
        val applied = TCRSDatabase.applyModifiers(11)
        assertEquals(1, applied)
    }

    @Test
    fun applyModifiers_overridesConsumableAdventures() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                foodItemId to TcrsEntry(
                    name = "TCRS Food",
                    size = 2,
                    quality = "good",
                    modifiers = "Meat Drop: +5",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("6", ConsumableDatabase.getAdventureRange(foodName))
        assertEquals(2, ConsumableDatabase.getFullnessByName(foodName))
        assertEquals("good", ConsumableDatabase.getQualityName(foodName))
    }

    @Test
    fun applyModifiers_spleenZeroAdventures() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                spleenItemId to TcrsEntry(
                    name = "TCRS Spleen",
                    size = 3,
                    quality = "awesome",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("0", ConsumableDatabase.getAdventureRange(spleenName))
        assertEquals(3, ConsumableDatabase.getSpleenByName(spleenName))
    }

    @Test
    fun applyModifiers_blankModifiersStillAppliesConsumable() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                foodItemId to TcrsEntry(
                    name = "TCRS Food",
                    size = 2,
                    quality = "decent",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("4", ConsumableDatabase.getAdventureRange(foodName))
        assertEquals(2, ConsumableDatabase.getFullnessByName(foodName))
    }

    @Test
    fun resetModifiers_restoresBundled() {
        TCRSDatabase.injectMapForTest(
            mapOf(testItemId to TcrsEntry(name = "TCRS Sword", modifiers = "Meat Drop: +10")),
        )
        TCRSDatabase.applyModifiers(11)
        assertTrue(ModifierDatabase.getItem(itemName)?.modifiers?.contains("Meat Drop: +10") == true)
        TCRSDatabase.resetModifiers(testPrefs(), 11)
        assertEquals("Meat Drop: +1", ModifierDatabase.getItem(itemName)?.modifiers)
    }

    @Test
    fun resetModifiers_restoresConsumableBundled() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                foodItemId to TcrsEntry(
                    name = "TCRS Food",
                    size = 2,
                    quality = "good",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("6", ConsumableDatabase.getAdventureRange(foodName))
        TCRSDatabase.resetModifiers(testPrefs(), 11)
        assertEquals("2-3", ConsumableDatabase.getAdventureRange(foodName))
        assertEquals(1, ConsumableDatabase.getFullnessByName(foodName))
    }

    @Test
    fun resetModifiers_noopWhenNotLoaded() {
        ModifierDatabase.injectForTest("Item", itemName, "Meat Drop: +1")
        ModifierDatabase.overrideModifier("Item", itemName, "Meat Drop: +99")
        TCRSDatabase.reset()
        TCRSDatabase.resetModifiers(testPrefs(), 11)
        assertEquals("Meat Drop: +99", ModifierDatabase.getItem(itemName)?.modifiers)
    }

    @Test
    fun applyModifiers_patchesEffectSourceForFoodWithEffect() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                effectFoodItemId to TcrsEntry(
                    name = "TCRS Effect Food",
                    size = 1,
                    quality = "decent",
                    modifiers = "Effect: $effectName",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals(
            "eat 1 $effectFoodName",
            EffectDatabase.getByName(effectName)?.actions,
        )
    }

    @Test
    fun resetModifiers_restoresEffectActions() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                effectFoodItemId to TcrsEntry(
                    name = "TCRS Effect Food",
                    size = 1,
                    quality = "decent",
                    modifiers = "Effect: $effectName",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("eat 1 $effectFoodName", EffectDatabase.getByName(effectName)?.actions)
        TCRSDatabase.resetModifiers(testPrefs(), 11)
        assertEquals("eat 1 old food", EffectDatabase.getByName(effectName)?.actions)
    }

    @Test
    fun applyModifiers_updatesConcoctionEffectName() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                effectFoodItemId to TcrsEntry(
                    name = "TCRS Effect Food",
                    size = 1,
                    quality = "decent",
                    modifiers = "Effect: $effectName",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals(effectName, ConcoctionDatabase.getEffectName(effectFoodName))
        TCRSDatabase.resetModifiers(testPrefs(), 11)
        assertEquals(null, ConcoctionDatabase.getEffectName(effectFoodName))
    }

    @Test
    fun applyModifiers_cafeBoozeMapAppliesDrinkAdventures() {
        val cafeId = -1
        val cafeName = "tcrs-cafe-porter"
        CafeDatabase.injectForTest(cafeId, cafeName, ConsumableType.DRINK)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = cafeName,
                type = ConsumableType.DRINK,
                amount = 3,
                levelReq = 4,
                quality = ConsumableQuality.DECENT,
                advMin = 3,
                advMax = 5,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "base",
            ),
        )
        TCRSDatabase.injectMapForTest(emptyMap())
        TCRSDatabase.injectCafeMapsForTest(
            booze = mapOf(
                cafeId to TcrsEntry(
                    name = "TCRS Porter",
                    size = 2,
                    quality = "good",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("6", ConsumableDatabase.getAdventureRange(cafeName))
        assertEquals(2, ConsumableDatabase.getInebrietyByName(cafeName))
    }

    @Test
    fun applyModifiers_cafeFoodMapAppliesFoodAdventures() {
        val cafeId = -1
        val cafeName = "tcrs-cafe-frog"
        CafeDatabase.injectForTest(cafeId, cafeName, ConsumableType.FOOD)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = cafeName,
                type = ConsumableType.FOOD,
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
                notes = "base",
            ),
        )
        TCRSDatabase.injectMapForTest(emptyMap())
        TCRSDatabase.injectCafeMapsForTest(
            food = mapOf(
                cafeId to TcrsEntry(
                    name = "TCRS Frog",
                    size = 3,
                    quality = "decent",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals("6", ConsumableDatabase.getAdventureRange(cafeName))
        assertEquals(3, ConsumableDatabase.getFullnessByName(cafeName))
    }

    private fun testPrefs(): Preferences = Preferences(MapSettings())
}
