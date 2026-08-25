package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Phases 1431–1445 Evaluator polish regression. */
class EvaluatorOsityWeaponTest {

    @Test
    fun clownosityParsesDefaultAndScores() {
        val eval = Evaluator("clownosity")
        assertEquals(100, eval.clownosityTarget())
        val mods = CurrentModifiers(
            CharacterState(),
            customModifierOverlay = "Clowniness",
        )
        // Bare bitmap tag contributes at least one bit; score capped at target
        val score = eval.getScore(mods)
        assertTrue(score >= 1.0)
        // With only 1 bit of clowniness vs target 100, failed
        assertTrue(eval.failed)
    }

    @Test
    fun raveosityAndSurgeonosityDefaults() {
        assertEquals(7, Evaluator("raveosity").raveosityTarget())
        assertEquals(5, Evaluator("surgeonosity").surgeonosityTarget())
    }

    @Test
    fun stinkycheeseParses() {
        val eval = Evaluator("3 stinkycheese")
        assertEquals(3, eval.stinkycheeseWeight())
    }

    @Test
    fun weaponKeywordsParse() {
        val eval = Evaluator("club, sword, knife, utensil, accordion, melee, type club, 1 hands")
        assertTrue(eval.requireClub())
        assertTrue(eval.requireSword())
        assertTrue(eval.requireKnife())
        assertTrue(eval.requireUtensil())
        assertTrue(eval.requireAccordion())
        assertEquals(2, eval.meleeConstraint())
        assertEquals("club", eval.weaponTypeFilter())
        assertEquals(1, eval.handsConstraint())
    }

    @Test
    fun loadoutBooleanFailForSea() {
        val eval = Evaluator("muscle")
        eval.applyBooleanConstraints(
            setOf(BooleanModifier.ADVENTURE_UNDERWATER, BooleanModifier.UNDERWATER_FAMILIAR),
            emptySet(),
        )
        val incomplete = CurrentModifiers(
            CharacterState(),
            customModifierOverlay = "Adventure Underwater",
        )
        eval.getScore(incomplete)
        assertTrue(eval.failed)

        val complete = CurrentModifiers(
            CharacterState(),
            customModifierOverlay = "Adventure Underwater, Underwater Familiar",
        )
        eval.getScore(complete)
        assertFalse(eval.failed)
    }

    @Test
    fun rolloverEffectAddsFudge() {
        val bare = Evaluator("muscle").getScore(CurrentModifiers(CharacterState()))
        val withRollover = Evaluator("muscle").getScore(
            CurrentModifiers(
                CharacterState(),
                customModifierOverlay = "Rollover Effect: \"Foo\"",
            ),
        )
        assertTrue(withRollover >= bare)
    }
}

class MaximizerWeaponGatesTest {
    @Test
    fun chefstaffRequiresEligibility() {
        assertFalse(
            MaximizerWeaponGates.canUseChefstaff(
                CharacterState(),
                hasSkill = { false },
                gloveAvailable = false,
            ),
        )
        assertTrue(
            MaximizerWeaponGates.canUseChefstaff(
                CharacterState(),
                hasSkill = { it == MaximizerWeaponGates.SPIRIT_OF_RIGATONI },
                gloveAvailable = false,
            ),
        )
        assertTrue(
            MaximizerWeaponGates.canUseChefstaff(
                CharacterState(characterClass = CharacterClass.SAUCEROR.id),
                hasSkill = { false },
                gloveAvailable = true,
            ),
        )
    }
}

class MaximizerAccessoryDedupTest {
    @Test
    fun accessorySlotsDedupEachOther() {
        val slots = MaximizerSpeculation.crossSlotDedupSlots(
            net.sourceforge.kolmafia.character.EquipmentSlot.ACC2,
        )
        assertTrue(net.sourceforge.kolmafia.character.EquipmentSlot.ACC1 in slots)
        assertTrue(net.sourceforge.kolmafia.character.EquipmentSlot.ACC3 in slots)
    }

    @Test
    fun holsterInSearchSlots() {
        assertTrue(
            net.sourceforge.kolmafia.character.EquipmentSlot.HOLSTER in
                net.sourceforge.kolmafia.character.EquipmentSlot.SEARCH_SLOTS,
        )
    }
}
