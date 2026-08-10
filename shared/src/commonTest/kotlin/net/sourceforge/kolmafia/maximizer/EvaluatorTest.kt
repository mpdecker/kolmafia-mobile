package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.ModifierValues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvaluatorTest {

    @Test
    fun addFudge_itemGoal_propagatesMicroWeights() {
        val eval = Evaluator("item")
        assertEquals(0.0001, eval.weightOf(DoubleModifier.SPORADIC_ITEMDROP))
        assertEquals(0.0001, eval.weightOf(DoubleModifier.FAIRY_WEIGHT))
    }

    @Test
    fun addFudge_zeroItemWeight_skipsItemClusterExtras() {
        val eval = Evaluator("meat")
        assertEquals(0.0, eval.weightOf(DoubleModifier.SPORADIC_ITEMDROP))
        assertEquals(0.0, eval.weightOf(DoubleModifier.FAIRY_WEIGHT))
        assertTrue(eval.weightOf(DoubleModifier.SPORADIC_MEATDROP) > 0.0)
    }

    @Test
    fun checkConstraints_violatesWhenForbiddenBooleanPresent() {
        val eval = Evaluator("muscle")
        eval.applyBooleanConstraints(emptySet(), setOf(BooleanModifier.VOLLEYBALL_OR_SOMBRERO))
        val mods = ModifierValues(booleans = mapOf(BooleanModifier.VOLLEYBALL_OR_SOMBRERO to true))
        assertEquals(Evaluator.Constraint.VIOLATES, eval.checkConstraints(mods))
    }

    @Test
    fun checkConstraints_meetsWhenRequiredBooleanPresent() {
        val eval = Evaluator("muscle")
        eval.applyBooleanConstraints(setOf(BooleanModifier.VOLLEYBALL_OR_SOMBRERO), emptySet())
        val mods = ModifierValues(booleans = mapOf(BooleanModifier.VOLLEYBALL_OR_SOMBRERO to true))
        assertEquals(Evaluator.Constraint.MEETS, eval.checkConstraints(mods))
    }

    @Test
    fun checkConstraints_irrelevantWhenNoMaskedBooleansActive() {
        val eval = Evaluator("muscle")
        eval.applyBooleanConstraints(setOf(BooleanModifier.VOLLEYBALL_OR_SOMBRERO), emptySet())
        assertEquals(Evaluator.Constraint.IRRELEVANT, eval.checkConstraints(ModifierValues.EMPTY))
    }

    @Test
    fun getScore_setsFailedWhenMinNotMet() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val eval = Evaluator("1 muscle, 50 min")
        val state = CharacterState(baseMusc = 10)
        eval.getScore(CurrentModifiers(state))
        assertTrue(eval.failed, "loadout muscle below min should set failed")
    }

    @Test
    fun getScore_clearsFailedWhenMinMet() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val eval = Evaluator("1 muscle, 50 min")
        val state = CharacterState(
            baseMusc = 100,
            equipment = mapOf(EquipmentSlot.WEAPON to "muculent machete"),
        )
        eval.getScore(CurrentModifiers(state))
        assertFalse(eval.failed, "loadout muscle above min should not set failed")
    }

    @Test
    fun parse_weightedGoal() {
        val eval = Evaluator("2 item, 1 meat")
        assertEquals(2.0, eval.weightOf(DoubleModifier.ITEMDROP))
        assertEquals(1.0, eval.weightOf(DoubleModifier.MEATDROP))
    }

    @Test
    fun parse_minMax() {
        val eval = Evaluator("0.1 DA 1000 max")
        assertEquals(0.1, eval.weightOf(DoubleModifier.DAMAGE_ABSORPTION))
        assertEquals(1000.0, eval.maxOf(DoubleModifier.DAMAGE_ABSORPTION))
    }

    @Test
    fun parse_standaloneMinSetsTotalMin() {
        val eval = Evaluator("500 min")
        assertEquals(500.0, eval.totalMin())
    }

    @Test
    fun parse_standaloneMaxSetsTotalMax() {
        val eval = Evaluator("1000 max")
        assertEquals(1000.0, eval.totalMax())
    }

    @Test
    fun getScore_totalMinSetsFailed() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val eval = Evaluator("500 min")
        eval.getScore(CurrentModifiers(CharacterState()))
        assertTrue(eval.failed, "total score below standalone min should set failed")
    }

    @Test
    fun getScore_setsExceededWhenTotalMaxReached() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val eval = Evaluator("10 max, 1 meat")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.WEAPON to "flamingo mallet"),
        )
        eval.getScore(CurrentModifiers(state))
        assertTrue(eval.exceeded, "high meat score should exceed totalMax=10")
    }

    @Test
    fun parse_allRes() {
        val eval = Evaluator("0.5 all res")
        for (mod in listOf(
            DoubleModifier.COLD_RESISTANCE,
            DoubleModifier.HOT_RESISTANCE,
            DoubleModifier.SLEAZE_RESISTANCE,
            DoubleModifier.SPOOKY_RESISTANCE,
            DoubleModifier.STENCH_RESISTANCE,
        )) {
            assertEquals(0.5, eval.weightOf(mod), "weight for $mod")
        }
    }

    @Test
    fun getScore_meatdropBaseline() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.WEAPON to "flamingo mallet"),
        )
        val eval = Evaluator("meat")
        val score = eval.getScore(CurrentModifiers(state))
        assertTrue(score >= 105.0, "meat drop baseline + modifier expected, got $score")
    }

    @Test
    fun getScore_buffedMuscle() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState(
            baseMusc = 10,
            equipment = mapOf(EquipmentSlot.WEAPON to "muculent machete"),
        )
        val eval = Evaluator("mus")
        val score = eval.getScore(CurrentModifiers(state))
        assertTrue(score >= 12.0, "buffed muscle should include base + item, got $score")
    }

    @Test
    fun tiebreaker_differsFromHardcoded() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val state = CharacterState()
        val light = mapOf(EquipmentSlot.HAT to ("papier-mitre" to 0.0))
        val heavy = mapOf(EquipmentSlot.HAT to ("crumpled felt fedora" to 0.0))

        val lightMods = CurrentModifiers(state.copy(equipment = mapOf(EquipmentSlot.HAT to "papier-mitre")))
        val heavyMods = CurrentModifiers(state.copy(equipment = mapOf(EquipmentSlot.HAT to "crumpled felt fedora")))
        val oldLight = oldHardcodedTiebreaker(lightMods)
        val oldHeavy = oldHardcodedTiebreaker(heavyMods)
        assertEquals(oldLight, oldHeavy, "old tiebreaker ignores familiar weight")

        val newLight = Evaluator.tiebreaker().getScore(lightMods)
        val newHeavy = Evaluator.tiebreaker().getScore(heavyMods)
        assertTrue(newHeavy > newLight, "desktop tiebreaker prefers higher familiar weight")
    }

    @Test
    fun estimatedBaseExp_matchesDesktopFormula() {
        assertEquals(2.25, Evaluator.estimatedBaseExp(monsterLevel = 3.0, zoneMl = 5.0))
    }

    @Test
    fun getItemContribution_weightedSumWithoutBaseline() {
        val eval = Evaluator("2 item, 1 meat")
        val itemValues = ModifierParser.parse("Item Drop: +20")
        val meatValues = ModifierParser.parse("Meat Drop: +10")
        assertEquals(40.0, eval.getItemContribution(itemValues))
        assertEquals(10.0, eval.getItemContribution(meatValues))
    }

    @Test
    fun highestWeightedStat_prefersItemDropOnTie() {
        val eval = Evaluator("1 item, 1 meat")
        assertEquals(DoubleModifier.ITEMDROP, eval.highestWeightedStat())
    }

    @Test
    fun getItemContribution_ignoresMeatdropBaseline() = runBlocking {
        ModifierDatabase.load()
        OutfitDatabase.load()
        val eval = Evaluator("meat")
        val loadoutScore = eval.getScore(
            CurrentModifiers(
                CharacterState(equipment = mapOf(EquipmentSlot.WEAPON to "flamingo mallet")),
            ),
        )
        val itemOnly = eval.getItemContribution(
            ModifierParser.parse("Meat Drop: +5"),
        )
        assertTrue(loadoutScore >= 105.0)
        assertEquals(5.0, itemOnly)
    }

    private fun oldHardcodedTiebreaker(mods: CurrentModifiers): Double {
        val v = mods.values
        return v.get(DoubleModifier.INITIATIVE) +
            v.get(DoubleModifier.ITEMDROP) +
            v.get(DoubleModifier.MUS) +
            v.get(DoubleModifier.MYS) +
            v.get(DoubleModifier.MOX) +
            v.get(DoubleModifier.MEATDROP)
    }
}
