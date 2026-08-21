package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Clan Photo Booth choices 1534 / 1535 —
 * effect and prop daily counters from choice.php posts.
 */
object PhotoBoothChoiceSync {

    const val EFFECT_CHOICE = 1534
    const val PROP_CHOICE = 1535

    const val EFFECTS_PREF = "_photoBoothEffects"
    const val EQUIPMENT_PREF = "_photoBoothEquipment"

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            EFFECT_CHOICE -> {
                if (decision == 6 || !html.contains("You select")) return false
                preferences.setInt(EFFECTS_PREF, preferences.getInt(EFFECTS_PREF, 0) + 1)
                true
            }
            PROP_CHOICE -> {
                if (decision == 12 || !html.contains("You grab your prop")) return false
                preferences.setInt(EQUIPMENT_PREF, preferences.getInt(EQUIPMENT_PREF, 0) + 1)
                true
            }
            else -> false
        }
    }
}
