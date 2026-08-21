package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] inspecting Motorbike choice 871 visit prefs.
 * Defers desert-beach / island unlock side effects.
 */
object MotorbikeChoiceSync {

    const val CHOICE_ID = 871

    private val PARTS = listOf(
        Regex("""<b>Tires:</b> (.*?)?\(""") to "peteMotorbikeTires",
        Regex("""<b>Gas Tank:</b> (.*?)?\(""") to "peteMotorbikeGasTank",
        Regex("""<b>Headlight:</b> (.*?)?\(""") to "peteMotorbikeHeadlight",
        Regex("""<b>Cowling:</b> (.*?)?\(""") to "peteMotorbikeCowling",
        Regex("""<b>Muffler:</b> (.*?)?\(""") to "peteMotorbikeMuffler",
        Regex("""<b>Seat:</b> (.*?)?\(""") to "peteMotorbikeSeat",
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        for ((pattern, pref) in PARTS) {
            val value = pattern.find(html)?.groupValues?.getOrNull(1)?.trim()
            if (!value.isNullOrEmpty()) {
                preferences.setString(pref, value)
                changed = true
            }
        }
        return changed
    }
}
