package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleGingerbreadCityChange] place/adventure pref writers.
 */
object GingerbreadCitySync {

    val ADVENTURE_IDS = setOf(477, 478, 479, 480, 481)

    fun applyFromVisit(
        url: String?,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val location = url.orEmpty()
        val isGinger =
            location.contains("whichplace=gingerbreadcity", ignoreCase = true) ||
                location.contains("snarfblat=477") ||
                location.contains("snarfblat=478") ||
                location.contains("snarfblat=479") ||
                location.contains("snarfblat=480") ||
                location.contains("snarfblat=481") ||
                html.contains("snarfblat=480") ||
                html.contains("snarfblat=481") ||
                html.contains("digitalclock.gif") ||
                html.contains("Infrastructure Failure")
        if (!isGinger &&
            !(location.contains("adventure.php", ignoreCase = true) &&
                SNARF.find(location)?.groupValues?.getOrNull(1)?.toIntOrNull() in ADVENTURE_IDS)
        ) {
            return false
        }
        var changed = false
        if (!preferences.getBoolean("gingerbreadCityAvailable", false) &&
            !html.contains("That's not a real place.")
        ) {
            preferences.setBoolean("_gingerbreadCityToday", true)
            changed = true
        }
        if (html.contains("snarfblat=480")) {
            preferences.setBoolean("gingerRetailUnlocked", true)
            changed = true
        }
        if (html.contains("snarfblat=481")) {
            preferences.setBoolean("gingerSewersUnlocked", true)
            changed = true
        }
        if (html.contains("digitalclock.gif")) {
            preferences.setBoolean("gingerAdvanceClockUnlocked", true)
            changed = true
        }
        if (html.contains("Infrastructure Failure")) {
            preferences.setInt(
                "_gingerbreadCityTurns",
                preferences.getInt("_gingerbreadCityTurns", 0) + 1,
            )
            changed = true
        }
        return changed
    }

    private val SNARF = Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE)
}
