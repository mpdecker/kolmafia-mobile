package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop KoLCharacter.canEat/canDrink/canChew + capacity getters (path/mode eligibility only). */
object ConsumptionEligibility {

    private const val REPLACEMENT_STOMACH = 17028
    private const val REPLACEMENT_LIVER = 17029

    private val edPaths = setOf(AscensionPath.ED, AscensionPath.ACTUALLY_ED_THE_UNDYING)

    fun canEat(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean {
        if (LimitModeGates.limitEating(state.limitMode)) return false
        if (state.ascensionPath in edPaths && !hasSkill(skills, REPLACEMENT_STOMACH)) return false
        if (state.inNoobcore) return false
        if (state.ascensionPath == AscensionPath.OXYGENARIAN ||
            state.ascensionPath == AscensionPath.BOOZETAFARIAN
        ) {
            return false
        }
        return true
    }

    fun canDrink(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean {
        if (LimitModeGates.limitDrinking(state.limitMode)) return false
        if (state.ascensionPath in edPaths && !hasSkill(skills, REPLACEMENT_LIVER)) return false
        if (state.inNoobcore) return false
        if (state.ascensionPath == AscensionPath.PLUMBER ||
            state.ascensionPath == AscensionPath.YOU_ROBOT ||
            state.ascensionPath == AscensionPath.MEAT
        ) {
            return false
        }
        if (state.ascensionPath == AscensionPath.OXYGENARIAN ||
            state.ascensionPath == AscensionPath.TEETOTALER
        ) {
            return false
        }
        return true
    }

    fun canChew(state: CharacterState): Boolean {
        if (LimitModeGates.limitSpleening(state.limitMode)) return false
        if (state.inNoobcore) return false
        if (state.ascensionPath == AscensionPath.YOU_ROBOT ||
            state.ascensionPath == AscensionPath.MEAT
        ) {
            return false
        }
        return true
    }

    fun stomachCapacity(
        state: CharacterState,
        skills: List<SkillData> = emptyList(),
        modifiers: CurrentModifiers? = null,
    ): Int {
        if (!canEat(state, skills)) return 0
        val base = state.ascensionPath.stomachCapacity
        val bonus = modifiers?.values?.get(DoubleModifier.STOMACH_CAPACITY)?.toInt() ?: 0
        return base + bonus
    }

    fun liverCapacity(
        state: CharacterState,
        skills: List<SkillData> = emptyList(),
        modifiers: CurrentModifiers? = null,
    ): Int {
        if (!canDrink(state, skills)) return 0
        val base = state.ascensionPath.liverCapacity
        val bonus = modifiers?.values?.get(DoubleModifier.LIVER_CAPACITY)?.toInt() ?: 0
        return base + bonus
    }

    fun spleenCapacity(state: CharacterState): Int =
        if (canChew(state)) state.ascensionPath.spleenCapacity else 0

    fun effectiveFullnessRemaining(
        state: CharacterState,
        skills: List<SkillData> = emptyList(),
        modifiers: CurrentModifiers? = null,
    ): Int = (
        stomachCapacity(state, skills, modifiers) -
            state.fullness -
            ConcoctionDatabase.getQueuedFullness()
        ).coerceAtLeast(0)

    fun effectiveInebrietyRemaining(
        state: CharacterState,
        skills: List<SkillData> = emptyList(),
        modifiers: CurrentModifiers? = null,
    ): Int = (
        liverCapacity(state, skills, modifiers) -
            state.inebriety -
            ConcoctionDatabase.getQueuedInebriety()
        ).coerceAtLeast(0)

    fun effectiveSpleenRemaining(state: CharacterState): Int = (
        spleenCapacity(state) - state.spleenUsed - ConcoctionDatabase.getQueuedSpleenHit()
        ).coerceAtLeast(0)

    fun canExpandStomach(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean =
        CharacterCapacity.canExpandStomachCapacity(state, skills)

    fun canExpandLiver(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean =
        CharacterCapacity.canExpandLiverCapacity(state, skills)

    private fun hasSkill(skills: List<SkillData>, skillId: Int): Boolean =
        skills.any { it.id == skillId }
}
