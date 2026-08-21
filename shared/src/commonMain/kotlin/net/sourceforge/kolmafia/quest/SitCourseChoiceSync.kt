package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Examine S.I.T. Course Certificate choice 1494.
 */
object SitCourseChoiceSync {

    const val CHOICE_ID = 1494

    const val CRYPTOBOTANIST_ID = 218
    const val INSECTOLOGIST_ID = 219
    const val PSYCHOGEOLOGIST_ID = 220

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        removeSkill: (Int) -> Unit = {},
        learnSkill: (Int) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val (skillName, keepId, removeIds) = when (decision) {
            1 -> Triple("Psychogeologist", PSYCHOGEOLOGIST_ID, listOf(INSECTOLOGIST_ID, CRYPTOBOTANIST_ID))
            2 -> Triple("Insectologist", INSECTOLOGIST_ID, listOf(PSYCHOGEOLOGIST_ID, CRYPTOBOTANIST_ID))
            3 -> Triple("Cryptobotanist", CRYPTOBOTANIST_ID, listOf(PSYCHOGEOLOGIST_ID, INSECTOLOGIST_ID))
            else -> return false
        }
        removeIds.forEach { id ->
            removeSkill(id)
            preferences.setInt("skillLevel$id", 0)
        }
        learnSkill(keepId)
        preferences.setInt("skillLevel$keepId", 1)
        preferences.setString("currentSITSkill", skillName)
        preferences.setBoolean("_sitCourseCompleted", true)
        return true
    }
}
