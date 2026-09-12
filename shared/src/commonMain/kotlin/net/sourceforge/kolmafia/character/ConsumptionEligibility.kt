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

    /**
     * Desktop AscensionClass organ overrides keyed by live API class id.
     * Null means fall through to [AscensionPath] capacity (desktop `getCapacity`).
     */
    private data class ClassOrganCaps(
        val stomach: Int? = null,
        val liver: Int? = null,
        val spleen: Int? = null,
    )

    private val classOrganCapsById = mapOf(
        11 to ClassOrganCaps(stomach = 20, liver = 4), // Avatar of Boris
        12 to ClassOrganCaps(liver = 4), // Zombie Master
        14 to ClassOrganCaps(stomach = 10, liver = 9), // Avatar of Jarlsberg
        15 to ClassOrganCaps(stomach = 5, liver = 19), // Avatar of Sneaky Pete
        17 to ClassOrganCaps(stomach = 0, liver = 0, spleen = 5), // Ed the Undying
        18 to ClassOrganCaps(stomach = 10, liver = 9, spleen = 10), // Cow Puncher
        19 to ClassOrganCaps(stomach = 10, liver = 9, spleen = 10), // Beanslinger
        20 to ClassOrganCaps(stomach = 10, liver = 9, spleen = 10), // Snake Oiler
        23 to ClassOrganCaps(stomach = 0, liver = 0), // Gelatinous Noob
        24 to ClassOrganCaps(stomach = 5, liver = 4), // Vampyre
        25 to ClassOrganCaps(stomach = 20, liver = 0, spleen = 5), // Plumber
        27 to ClassOrganCaps(stomach = 0, liver = 0, spleen = 0), // Grey Goo
    )

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
            state.ascensionPath == AscensionPath.PATH_OF_THE_PLUMBER ||
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
            state.ascensionPath == AscensionPath.MEAT ||
            state.ascensionPath == AscensionPath.GREY_YOU
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
        val base = classOrganCapsById[state.characterClass]?.stomach
            ?: state.ascensionPath.stomachCapacity
        val bonus = modifiers?.values?.get(DoubleModifier.STOMACH_CAPACITY)?.toInt() ?: 0
        return base + bonus
    }

    fun liverCapacity(
        state: CharacterState,
        skills: List<SkillData> = emptyList(),
        modifiers: CurrentModifiers? = null,
    ): Int {
        if (!canDrink(state, skills)) return 0
        val base = classOrganCapsById[state.characterClass]?.liver
            ?: state.ascensionPath.liverCapacity
        val bonus = modifiers?.values?.get(DoubleModifier.LIVER_CAPACITY)?.toInt() ?: 0
        return base + bonus
    }

    fun spleenCapacity(
        state: CharacterState,
        modifiers: CurrentModifiers? = null,
    ): Int {
        if (!canChew(state)) return 0
        val base = classOrganCapsById[state.characterClass]?.spleen
            ?: state.ascensionPath.spleenCapacity
        val bonus = modifiers?.values?.get(DoubleModifier.SPLEEN_CAPACITY)?.toInt() ?: 0
        return base + bonus
    }

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

    fun effectiveSpleenRemaining(
        state: CharacterState,
        modifiers: CurrentModifiers? = null,
    ): Int = (
        spleenCapacity(state, modifiers) - state.spleenUsed - ConcoctionDatabase.getQueuedSpleenHit()
        ).coerceAtLeast(0)

    fun canExpandStomach(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean =
        CharacterCapacity.canExpandStomachCapacity(state, skills)

    fun canExpandLiver(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean =
        CharacterCapacity.canExpandLiverCapacity(state, skills)

    private fun hasSkill(skills: List<SkillData>, skillId: Int): Boolean =
        skills.any { it.id == skillId }
}
