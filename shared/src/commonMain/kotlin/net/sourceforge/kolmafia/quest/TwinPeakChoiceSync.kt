package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Twin Peak rooms 607, 608, 616–618, 1056.
 */
object TwinPeakChoiceSync {

    const val ROOM_237 = 607
    const val GO_CHECK_IT_OUT = 608
    const val HE_IS_THE_ARM = 616
    const val NOW_ITS_DARK = 617
    const val CABIN_FEVER = 618
    const val NOW_ITS_DARK_ALT = 1056
    const val JAR_OF_OIL = 5353
    const val PREF = "twinPeakProgress"

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        val prefs = preferences ?: return false
        return when (choiceId) {
            ROOM_237 -> {
                if (!html.contains("You take a moment to steel your nerves.")) return false
                orProgress(prefs, 1)
                true
            }
            GO_CHECK_IT_OUT -> {
                if (!html.contains("All work and no play")) return false
                orProgress(prefs, 2)
                true
            }
            HE_IS_THE_ARM -> {
                if (!html.contains("You attempt to mingle")) return false
                orProgress(prefs, 4)
                consumeItem(JAR_OF_OIL, 1)
                true
            }
            NOW_ITS_DARK, NOW_ITS_DARK_ALT -> {
                if (!html.contains("When the lights come back")) return false
                prefs.setInt(PREF, 15)
                true
            }
            CABIN_FEVER -> {
                if (!html.contains("mercifully, the hotel explodes")) return false
                prefs.setInt(PREF, 15)
                true
            }
            else -> false
        }
    }

    private fun orProgress(preferences: Preferences, bit: Int) {
        preferences.setInt(PREF, preferences.getInt(PREF, 0) or bit)
    }
}
