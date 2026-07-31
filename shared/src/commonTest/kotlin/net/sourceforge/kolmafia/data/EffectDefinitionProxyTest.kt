package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EffectDefinitionProxyTest {

    @AfterTest
    fun tearDown() {
        EffectDatabase.resetForTest()
    }

    @Test
    fun getAllActions_expandsUseEither() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 3,
                name = "Confused",
                image = "confused.gif",
                descId = "test",
                quality = EffectQuality.BAD,
                attributes = emptySet(),
                actions = "use either 1 Now and Earlier, 1 Senior Mints, 1 shingle|eat 1 herb brownies",
            ),
        )
        assertEquals(
            listOf(
                "use 1 Now and Earlier",
                "use 1 Senior Mints",
                "use 1 shingle",
                "eat 1 herb brownies",
            ),
            EffectDefinitionProxy.getAllActions(3),
        )
    }

    @Test
    fun getActionNote_hashPrefixedActions() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 59,
                name = "Wanged",
                image = "wang.gif",
                descId = "test",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
                actions = "# wang used on you",
            ),
        )
        assertEquals("wang used on you", EffectDefinitionProxy.getActionNote(59))
        assertNull(EffectDefinitionProxy.getDefaultAction(59))
    }

    @Test
    fun getDefaultAction_firstPipeSegment() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 2,
                name = "Sleepy",
                image = "sleepy.gif",
                descId = "test",
                quality = EffectQuality.BAD,
                attributes = emptySet(),
                actions = "use 1 decorative fountain|eat 1 hippy herbal tea",
            ),
        )
        assertEquals("use 1 decorative fountain", EffectDefinitionProxy.getDefaultAction(2))
    }

    @Test
    fun candyEffectTier_synthesisIds() {
        assertEquals(1, CandyEffectTier.getEffectTier(2165))
        assertEquals(2, CandyEffectTier.getEffectTier(2170))
        assertEquals(3, CandyEffectTier.getEffectTier(2175))
        assertEquals(0, CandyEffectTier.getEffectTier(9999))
    }

    @Test
    fun getAllActions_reflectsTcrsPatchedSource() {
        val effectName = "Proxy Tcrs Effect"
        val foodName = "proxy-tcrs-food"
        EffectDatabase.registerForTest(
            EffectData(
                id = 9_000_020,
                name = effectName,
                image = "proxy.gif",
                descId = "proxy",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 old food",
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 9_000_021,
                name = foodName,
                descId = "proxy-food",
                image = "food.gif",
                primaryUse = ItemPrimaryUse.FOOD,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", foodName, "Meat Drop: +1")
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
        TCRSDatabase.injectMapForTest(
            mapOf(
                9_000_021 to TCRSDatabase.TcrsEntry(
                    name = "TCRS Proxy Food",
                    size = 1,
                    quality = "decent",
                    modifiers = "Effect: $effectName",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(11)
        assertEquals(
            listOf("eat 1 $foodName"),
            EffectDefinitionProxy.getAllActions(9_000_020),
        )
        TCRSDatabase.reset()
        ModifierDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }
}
