package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [JuneCleaverManager.parseChoice] for choices 1467–1475.
 */
object JuneCleaverChoiceSync {

    val CHOICE_IDS = (1467..1475).toSet()

    private val NORMAL_FIGHTS_TO_CHOICE = intArrayOf(1, 6, 10, 12, 15, 20, 30)
    private val RESET_FIGHTS_TO_CHOICE = intArrayOf(1, 2, 3, 3, 4, 5, 8)

    private val WHICH_CHOICE = Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
    private val OPTION = Regex("""option=(\d+)""", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (preferences == null) return false
        val id = WHICH_CHOICE.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: choiceId
        if (id !in CHOICE_IDS) return false
        val option = OPTION.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: decision
        if (option !in 1..4) return false

        updateQueue(preferences, id)
        when (option) {
            4 -> {
                preferences.setInt(
                    "_juneCleaverSkips",
                    preferences.getInt("_juneCleaverSkips", 0) + 1,
                )
                preferences.setInt("_juneCleaverFightsLeft", fightsLeft(preferences, skip = true))
            }
            in 1..3 -> {
                preferences.setInt(
                    "_juneCleaverEncounters",
                    preferences.getInt("_juneCleaverEncounters", 0) + 1,
                )
                preferences.setInt("_juneCleaverFightsLeft", fightsLeft(preferences, skip = false))
                if (id == 1467 && option == 3) {
                    preferences.setInt(
                        "_juneCleaverAdvs",
                        preferences.getInt("_juneCleaverAdvs", 0) + 5,
                    )
                }
            }
        }
        return true
    }

    private fun updateQueue(preferences: Preferences, id: Int) {
        val queue = preferences.getString("juneCleaverQueue", "")
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .toMutableList()
        queue.add(id)
        while (queue.size > 6) queue.removeAt(0)
        preferences.setString("juneCleaverQueue", queue.joinToString(","))
    }

    private fun fightsLeft(preferences: Preferences, skip: Boolean): Int {
        val fights = if (skip) RESET_FIGHTS_TO_CHOICE else NORMAL_FIGHTS_TO_CHOICE
        // After increment, encounters already includes this choice; desktop uses post-increment value
        val encounters = preferences.getInt("_juneCleaverEncounters", 0)
        return fights[minOf(encounters, fights.size - 1)]
    }
}
