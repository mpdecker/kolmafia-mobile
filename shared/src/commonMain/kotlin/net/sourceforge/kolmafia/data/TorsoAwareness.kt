package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.skill.SkillData

/** Desktop KoLCharacter.isTorsoAware() — TORSO skill or BEST_DRESSED. */
object TorsoAwareness {

    private const val TORSO = 12
    private const val BEST_DRESSED = 15022

    fun hasTorsoAwareness(skills: List<SkillData>): Boolean =
        skills.any { it.id == TORSO || it.id == BEST_DRESSED }

    fun hasTorsoAwareness(hasSkill: (Int) -> Boolean): Boolean =
        hasSkill(TORSO) || hasSkill(BEST_DRESSED)
}
