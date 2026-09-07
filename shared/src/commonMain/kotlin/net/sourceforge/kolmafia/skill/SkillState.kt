package net.sourceforge.kolmafia.skill

data class SkillState(
    val skills: List<SkillData> = emptyList(),
    val isStale: Boolean = false,
    /** Permed skill id → hardcore (true) / softcore (false). */
    val permedSkills: Map<Int, Boolean> = emptyMap(),
)
