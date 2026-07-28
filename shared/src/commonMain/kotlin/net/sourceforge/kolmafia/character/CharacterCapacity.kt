package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.skill.SkillData

/** Desktop KoLCharacter.canExpandStomachCapacity / canExpandLiverCapacity path gates. */
object CharacterCapacity {

    fun canExpandStomachCapacity(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean {
        if (!ConsumptionEligibility.canEat(state, skills)) return false
        if (state.ascensionPath == AscensionPath.YOU_ROBOT) return false
        if (state.challengePath.equals("License to Adventure", ignoreCase = true)) return false
        if (state.ascensionPath == AscensionPath.MEAT) return false
        if (state.ascensionPath == AscensionPath.GREY_YOU) return false
        if (state.ascensionPath == AscensionPath.VAMPYRE) return false
        if (state.ascensionPath == AscensionPath.SMALL) return false
        return true
    }

    fun canExpandLiverCapacity(state: CharacterState, skills: List<SkillData> = emptyList()): Boolean {
        if (!ConsumptionEligibility.canDrink(state, skills)) return false
        if (state.ascensionPath == AscensionPath.GREY_YOU) return false
        if (state.ascensionPath == AscensionPath.VAMPYRE) return false
        if (state.ascensionPath == AscensionPath.SMALL) return false
        return true
    }
}
