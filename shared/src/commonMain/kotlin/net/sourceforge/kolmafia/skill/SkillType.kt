package net.sourceforge.kolmafia.skill

enum class SkillType {
    PASSIVE, NONCOMBAT, COMBAT, BUFF, SUMMON, OTHER;

    companion object {
        fun fromApiInt(type: Int): SkillType = when (type) {
            0, 11 -> PASSIVE
            1, 2, 13 -> NONCOMBAT
            3 -> COMBAT
            5, 6 -> BUFF
            4 -> SUMMON
            else -> OTHER
        }
    }
}
