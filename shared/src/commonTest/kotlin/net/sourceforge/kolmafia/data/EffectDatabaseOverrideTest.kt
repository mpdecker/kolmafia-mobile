package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EffectDatabaseOverrideTest {

    @AfterTest
    fun tearDown() {
        EffectDatabase.resetForTest()
    }

    @Test
    fun stripConsumableActions_removesEatAndDrinkClauses() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 100,
                name = "Test Effect",
                image = "test.gif",
                descId = "test",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 herb brownies|drink 1 beer|cast 1 skill",
            ),
        )
        EffectDatabase.stripConsumableActions()
        assertEquals("cast 1 skill", EffectDatabase.getById(100)?.actions)
    }

    @Test
    fun stripConsumableActions_skipsHashPrefixedNotes() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 101,
                name = "Note Effect",
                image = "note.gif",
                descId = "note",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
                actions = "# wang used on you",
            ),
        )
        EffectDatabase.stripConsumableActions()
        assertEquals("# wang used on you", EffectDatabase.getById(101)?.actions)
    }

    @Test
    fun addEffectSource_appendsWhenNoMatchingClause() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 200,
                name = "Food Buff",
                image = "buff.gif",
                descId = "buff",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 skill",
            ),
        )
        EffectDatabase.addEffectSource("TCRS Food", ItemPrimaryUse.FOOD, "Food Buff")
        assertEquals(
            "cast 1 skill|eat 1 TCRS Food",
            EffectDatabase.getById(200)?.actions,
        )
    }

    @Test
    fun addEffectSource_mergesIntoExistingEatEitherClause() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 201,
                name = "Merged Buff",
                image = "merged.gif",
                descId = "merged",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat either 1 old food",
            ),
        )
        EffectDatabase.addEffectSource("TCRS Food", ItemPrimaryUse.FOOD, "Merged Buff")
        assertEquals(
            "eat either 1 old food, 1 TCRS Food",
            EffectDatabase.getById(201)?.actions,
        )
    }

    @Test
    fun addEffectSource_convertsEatToEither() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 202,
                name = "Convert Buff",
                image = "convert.gif",
                descId = "convert",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 old food",
            ),
        )
        EffectDatabase.addEffectSource("TCRS Food", ItemPrimaryUse.FOOD, "Convert Buff")
        assertEquals(
            "eat either 1 old food, 1 TCRS Food",
            EffectDatabase.getById(202)?.actions,
        )
    }

    @Test
    fun resetOverrides_restoresBundledActions() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 300,
                name = "Reset Effect",
                image = "reset.gif",
                descId = "reset",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 pizza",
            ),
        )
        EffectDatabase.updateActions(300, "eat 1 patched")
        EffectDatabase.resetOverrides()
        assertEquals("eat 1 pizza", EffectDatabase.getById(300)?.actions)
    }

    @Test
    fun stripConsumableActions_clearsAllConsumableClauses() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 301,
                name = "Empty After Strip",
                image = "empty.gif",
                descId = "empty",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 pizza|drink 1 beer",
            ),
        )
        EffectDatabase.stripConsumableActions()
        assertNull(EffectDatabase.getById(301)?.actions)
    }
}
