package net.sourceforge.kolmafia.concoction

import net.sourceforge.kolmafia.character.CharacterState

/** Desktop [SkillPool.SUPER_COCKTAIL] / [SkillPool.MIXOLOGIST] gate for stills. */
object StillsAvailability {

    const val SUPER_COCKTAIL = 5018
    const val MIXOLOGIST = 15002

    fun gatedCount(
        state: CharacterState,
        hasSkill: (Int) -> Boolean,
        guildStoreOpen: Boolean,
    ): Int {
        if (!hasSkill(SUPER_COCKTAIL) && !hasSkill(MIXOLOGIST)) return 0
        if (!state.characterClassEnum.isMoxieBased) return 0
        if (!guildStoreOpen && !state.isSneakyPete) return 0
        val stills = state.stillsAvailable
        return if (stills < 0) 0 else stills
    }
}
