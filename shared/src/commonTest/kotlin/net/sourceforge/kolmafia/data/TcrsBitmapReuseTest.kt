package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.TCRSDatabase.TcrsEntry
import net.sourceforge.kolmafia.modifiers.BitmapModifier

class TcrsBitmapReuseTest {

    private val foodItemId = 9_100_001
    private val saucyFoodId = 9_100_002
    private val effectFoodId = 9_100_003
    private val foodName = "tcrs-notes-plain-food"
    private val saucyFoodName = "tcrs-notes-saucy-food"
    private val effectFoodName = "tcrs-notes-effect-food"
    private val effectName = "Tcrs Bitmap Effect"

    @BeforeTest
    fun setUp() {
        registerFood(foodItemId, foodName, notes = "Unspaded")
        registerFood(saucyFoodId, saucyFoodName, notes = "SAUCY")
        registerFood(effectFoodId, effectFoodName, notes = "Unspaded")
        ModifierDatabase.injectForTest("Item", foodName, "")
        ModifierDatabase.injectForTest("Item", saucyFoodName, "")
        ModifierDatabase.injectForTest(
            "Item",
            effectFoodName,
            "Effect: $effectName, Effect Duration: 5",
        )
        ModifierDatabase.injectForTest("Effect", effectName, "Muscle: +1")
        EffectDatabase.registerForTest(
            EffectData(
                id = 9_100_010,
                name = effectName,
                image = "foo.gif",
                descId = "tcrs-bitmap-effect",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 old food",
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
        ModifierDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        EffectDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun applyModifiers_plainConsumableNotesAreEmptyNotUnspaded() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                foodItemId to TcrsEntry(
                    name = "TCRS Plain",
                    size = 1,
                    quality = "decent",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        val notes = ConsumableDatabase.getNotesByName(foodName)
        assertFalse(notes.contains("Unspaded"), "plain leftover notes should not be Unspaded")
        assertTrue(notes.isEmpty(), "plain leftover notes should be empty, got '$notes'")
    }

    @Test
    fun applyModifiers_keepsLeftoverAttributeWithoutUnspadedPrefix() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                saucyFoodId to TcrsEntry(
                    name = "TCRS Saucy",
                    size = 1,
                    quality = "decent",
                    modifiers = "",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        val notes = ConsumableDatabase.getNotesByName(saucyFoodName)
        assertTrue(notes.contains("SAUCY"), "leftover attribute SAUCY should be kept")
        assertFalse(notes.startsWith("Unspaded"), "must not prefix Unspaded, got '$notes'")
        assertFalse(notes == "Unspaded")
    }

    @Test
    fun applyModifiers_effectNoteOmitsLeadingUnspaded() {
        TCRSDatabase.injectMapForTest(
            mapOf(
                effectFoodId to TcrsEntry(
                    name = "TCRS Effect Food",
                    size = 1,
                    quality = "decent",
                    modifiers = "Effect: $effectName, Effect Duration: 5",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        val notes = ConsumableDatabase.getNotesByName(effectFoodName)
        assertTrue(notes.contains(effectName), "effect leftover should remain, got '$notes'")
        assertFalse(notes.startsWith("Unspaded"), "must not prefix Unspaded, got '$notes'")
    }

    @Test
    fun getBitmapMask_reparsesSameClowninessLookup() {
        ModifierDatabase.resetOverrides()
        val lookup = "Item:[9100001]"
        val first = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        val second = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        assertEquals(first, second)
        assertEquals(1, first)
        assertFalse(ModifierDatabase.hasTooManyBitmapSources(BitmapModifier.CLOWNINESS))
    }

    @Test
    fun getBitmapMask_sameClowninessLookupDoesNotExhaustBits() {
        ModifierDatabase.resetOverrides()
        val lookup = "Item:[clown-reuse]"
        val first = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        repeat(40) {
            ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        }
        val afterReuse = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        val other = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, "Item:[clown-other]", 1)
        assertEquals(first, afterReuse)
        assertNotEquals(first, other)
        assertFalse(ModifierDatabase.hasTooManyBitmapSources(BitmapModifier.CLOWNINESS))
    }

    @Test
    fun resetOverrides_clearsBitmapMasksBySource() {
        val lookup = "Item:[clown-reset]"
        val before = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, "Item:[clown-reset-other]", 1)
        ModifierDatabase.resetOverrides()
        val after = ModifierDatabase.getBitmapMask(BitmapModifier.CLOWNINESS, lookup, 1)
        assertEquals(before, after)
        assertEquals(1, after)
        assertFalse(ModifierDatabase.hasTooManyBitmapSources(BitmapModifier.CLOWNINESS))
    }

    private fun registerFood(itemId: Int, name: String, notes: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = name,
                descId = "tcrs-$itemId",
                image = "food.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
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
                notes = notes,
            ),
        )
    }
}
