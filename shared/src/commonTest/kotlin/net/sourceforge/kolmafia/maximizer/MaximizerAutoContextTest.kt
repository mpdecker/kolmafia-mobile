package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.ModifierParser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerAutoContextTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
        OutfitDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
        OutfitDatabase.resetForTest()
    }

    @Test
    fun autoContext_hoboUsefulWhenMaxCatScores() = runBlocking {
        ModifierDatabase.load()
        val eval = Evaluator("meat")
        val ctx = MaximizerAutoContext.from(eval)
        assertTrue(ctx.isCatUseful("_hoboPower"))
    }

    @Test
    fun autoContext_outfitPiecePinnedWhenOutfitUseful() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val eval = Evaluator("mysticality")
        val ctx = MaximizerAutoContext.from(eval)
        val beanieMods = ModifierParser.parse("Mysticality: +1")
        assertTrue(ctx.shouldPinAutomatic("bugbear beanie", beanieMods))
    }

    @Test
    fun shouldPinAutomatic_synergyItemWhenMeatGoal() = runBlocking {
        ModifierDatabase.injectForTest("Item", "bewitching boots", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest("Item", "bitter bowtie", "Cold Resistance: +1, Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest(
            "Synergy",
            "bewitching boots/bitter bowtie",
            "Meat Drop: +10, Cold Resistance: +1",
        )
        val eval = Evaluator("meat")
        val ctx = MaximizerAutoContext.from(eval)
        val bootsMods = ModifierParser.parse("Meat Drop: +10, Synergetic")
        assertTrue(ctx.shouldPinAutomatic("bewitching boots", bootsMods))
        assertTrue("bewitching boots" in ctx.usefulSynergyItemNames)
    }

    @Test
    fun parse_bareSynergeticSetsBitmapOne() {
        val mods = ModifierParser.parse("Meat Drop: +10, Synergetic")
        assertEquals(1, mods.get(net.sourceforge.kolmafia.modifiers.BitmapModifier.SYNERGETIC))
    }
}
