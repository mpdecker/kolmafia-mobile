package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleSpacegateChange] open-today flag.
 */
object SpacegateVisitSync {

    const val SPACEGATE = 494

    fun applyFromVisit(
        url: String?,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (preferences.getBoolean("spacegateAlways", false)) return false
        val location = url.orEmpty()
        val isSpacegate =
            location.contains("whichplace=spacegate", ignoreCase = true) ||
                location.contains("snarfblat=$SPACEGATE") ||
                (location.contains("adventure.php", ignoreCase = true) &&
                    location.contains("snarfblat=$SPACEGATE"))
        if (!isSpacegate) return false
        // Desktop sets today even without blocked-check when routing through spacegate place
        preferences.setBoolean("_spacegateToday", true)
        return true
    }
}
