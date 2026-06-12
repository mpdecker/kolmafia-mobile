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
            else -> {
                val raw = step.removePrefix("step")
                if (raw.contains('.')) {
                    val parts = raw.split('.')
                    val major = parts[0].toIntOrNull()
                    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    if (major == null) -1 else major * 10 + minor
                } else {
                    val n = raw.toIntOrNull()
                    if (n == null) -1 else n * 10
                }
            }
        }

        /** Major step number for ASH `quest_step()` (step16.5 → 16). */
        fun questStepNumber(step: String): Int = when (step) {
            UNSTARTED -> -1
            STARTED   ->  0
            FINISHED  ->  Int.MAX_VALUE
            else      -> step.removePrefix("step").substringBefore('.').toIntOrNull() ?: -1
        }

        fun validateStep(step: String): String = when {
            step == UNSTARTED || step == STARTED || step == FINISHED -> step
            step.matches(Regex("step\\d+(\\.\\d+)?")) -> step
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

    fun progressFor(prefKey: String): String =
        preferences.getString(prefKey, UNSTARTED)

    fun progressFor(quest: Quest): String = getProgress(quest)

    fun isAtLeast(quest: Quest, step: String): Boolean =
        stepOrdinal(getProgress(quest)) >= stepOrdinal(step)

    fun isFinished(prefKey: String): Boolean =
        progressFor(prefKey) == FINISHED
}
