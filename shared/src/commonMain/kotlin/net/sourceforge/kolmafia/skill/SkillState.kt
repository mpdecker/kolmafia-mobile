package net.sourceforge.kolmafia.skill

data class SkillState(
    val skills: List<SkillData> = emptyList(),
    val isStale: Boolean = false
)
