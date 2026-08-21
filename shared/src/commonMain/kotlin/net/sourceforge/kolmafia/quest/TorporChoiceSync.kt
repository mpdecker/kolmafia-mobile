package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Torpor choice 1342 —
 * Vampyre skill learn/forget via sk[]= URL params (skills 24010–24038).
 */
object TorporChoiceSync {

    const val CHOICE_ID = 1342
    const val SKILL_BASE = 24000
    val SKILL_OFFSETS = 10..38

    fun apply(
        choiceId: Int,
        decision: Int,
        choiceUrl: String,
        hasSkill: (Int) -> Boolean,
        learnSkill: (Int) -> Unit,
        forgetSkill: (Int) -> Unit,
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || decision != 2) return false
        var changed = false
        for (offset in SKILL_OFFSETS) {
            val skillId = SKILL_BASE + offset
            val selected = choiceUrl.contains("sk[]=$offset")
            val known = hasSkill(skillId)
            when {
                selected && !known -> {
                    learnSkill(skillId)
                    sessionLog("You have learned skill $skillId")
                    changed = true
                }
                !selected && known -> {
                    forgetSkill(skillId)
                    sessionLog("You have forgotten skill $skillId")
                    changed = true
                }
            }
        }
        return changed
    }
}
