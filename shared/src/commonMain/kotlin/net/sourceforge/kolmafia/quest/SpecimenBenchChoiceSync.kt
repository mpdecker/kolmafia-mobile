package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Specimen Preparation Bench choice 1555.
 */
object SpecimenBenchChoiceSync {

    const val CHOICE_ID = 1555
    const val MAX_SPECIMENS = 11

    private val DONE_SO = Regex("""You have done so (\d+) time""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val spawned = DONE_SO.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        preferences.setInt("zootSpecimensPrepared", spawned)
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        addFamiliarNonCombatExperience: (Int) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        if (!html.contains("You inject the viscous liquid")) return false
        addFamiliarNonCombatExperience(20)
        val next = (preferences.getInt("zootSpecimensPrepared", 0) + 1).coerceAtMost(MAX_SPECIMENS)
        preferences.setInt("zootSpecimensPrepared", next)
        return true
    }
}
