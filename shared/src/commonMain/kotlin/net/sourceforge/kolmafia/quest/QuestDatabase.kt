package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

class QuestDatabase(private val preferences: Preferences) {

    companion object {
        const val UNSTARTED = "unstarted"
        const val STARTED   = "started"
        const val FINISHED  = "finished"

        fun stepOrdinal(step: String): Int = when (step) {
            UNSTARTED -> -1
            STARTED   ->  0
            FINISHED  ->  Int.MAX_VALUE
            else      -> step.removePrefix("step").toIntOrNull() ?: -1
        }

        fun validateStep(step: String): String = when {
            step == UNSTARTED || step == STARTED || step == FINISHED -> step
            step.matches(Regex("step\\d+")) -> step
            else -> UNSTARTED
        }
    }

    fun getProgress(quest: Quest): String =
        preferences.getString(quest.prefKey, UNSTARTED)

    fun setProgress(quest: Quest, step: String) =
        preferences.setString(quest.prefKey, step)

    fun setProgressByPrefKey(prefKey: String, step: String) =
        preferences.setString(prefKey, validateStep(step))

    fun isQuestLaterThan(quest: Quest, step: String): Boolean {
        val current = getProgress(quest)
        return stepOrdinal(current) > stepOrdinal(step)
    }

    fun isQuestFinished(quest: Quest): Boolean =
        getProgress(quest) == FINISHED
}
