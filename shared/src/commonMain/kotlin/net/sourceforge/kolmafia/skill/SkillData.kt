package net.sourceforge.kolmafia.skill

data class SkillData(
    val id: Int,
    val name: String,
    val type: SkillType,
    val mpCost: Int,
    val dailyLimit: Int,    // 0 = unlimited
    val timesCast: Int
) {
    val isActive: Boolean get() = type != SkillType.PASSIVE
    val canCastMore: Boolean get() = dailyLimit == 0 || timesCast < dailyLimit
}
