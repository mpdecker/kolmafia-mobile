package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.JuneCleaverChoiceSync

/**
 * Desktop [net.sourceforge.kolmafia.session.JuneCleaverManager] — fight-end color flashes
 * plus choice 1467–1475 queue/skip/encounter routing.
 */
object JuneCleaverManager {
    private val MESSAGES = listOf(
        Regex("""As the battle ends, your cleaver flashes bright <span style="color: ([^"]+)""""),
        Regex("""Out of the corner of your eye, you catch a glimpse of bright <span style="color: ([^"]+)""""),
        Regex("""You notice a glint of <span style="color: ([^"]+)""""),
        Regex("""Your cleaver sparkles with a startling <span style="color: ([^"]+)""""),
    )

    fun updatePreferences(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        for (message in MESSAGES) {
            val match = message.find(html) ?: continue
            val color = match.groupValues.getOrNull(1) ?: continue
            preferences.setInt(
                "_juneCleaverFightsLeft",
                (preferences.getInt("_juneCleaverFightsLeft", 0) - 1).coerceAtLeast(0),
            )
            when (color) {
                "blue" -> increment(preferences, "_juneCleaverCold")
                "blueviolet" -> increment(preferences, "_juneCleaverSleaze")
                "gray" -> increment(preferences, "_juneCleaverSpooky")
                "green" -> increment(preferences, "_juneCleaverStench")
                "red" -> increment(preferences, "_juneCleaverHot")
            }
            return true
        }
        return false
    }

    fun parseChoice(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean = JuneCleaverChoiceSync.apply(choiceId, decision, preferences, choiceUrl)

    private fun increment(preferences: Preferences, key: String) {
        preferences.setInt(key, preferences.getInt(key, 0) + 1)
    }
}
