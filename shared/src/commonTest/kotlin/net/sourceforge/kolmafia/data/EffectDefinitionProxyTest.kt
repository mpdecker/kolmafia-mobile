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
}
