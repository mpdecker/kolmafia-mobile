package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Summon a Wave choice 1566.
 */
object SeadentWaveChoiceSync {

    const val CHOICE_ID = 1566

    private val SUMMON_WAVE = Regex(
        """sweep it down and point at (.*?)\.  A huge wave rises from the sea""",
        RegexOption.IGNORE_CASE,
    )

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        val zone = SUMMON_WAVE.find(html)?.groupValues?.getOrNull(1)?.trim() ?: return false
        preferences.setString("_seadentWaveZone", zone)
        preferences.setBoolean("_seadentWaveUsed", true)
        return true
    }
}
