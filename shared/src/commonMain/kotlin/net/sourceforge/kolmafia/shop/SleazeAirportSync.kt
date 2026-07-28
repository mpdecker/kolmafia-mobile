package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [QuestManager] sleaze-airport visit detection + Spring Beach ticket use. */
object SleazeAirportSync {

    const val PREF = "_sleazeAirportToday"
    const val SPRING_BEACH_TICKET = 7467

    private val SBB_SNARFBLETS = setOf("402", "403", "404")

    private val BLOCKED_MARKERS = listOf(
        "You don't know where that is.",
        "That isn't a place you can go.",
    )

    private val SNARFBLEAT_PATTERN = Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE)

    fun syncFromVisit(html: String, url: String?, prefs: Preferences) {
        if (prefs.getBoolean("sleazeAirportAlways", false)) return
        if (BLOCKED_MARKERS.any { html.contains(it) }) return
        if (isSleazeAdventure(url) || isSleazePlace(url, html)) {
            prefs.setBoolean(PREF, true)
        }
    }

    fun syncFromSpringBeachTicketUse(html: String, prefs: Preferences) {
        if (html.contains("already have access", ignoreCase = true)) return
        prefs.setBoolean(PREF, true)
    }

    private fun isSleazeAdventure(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.contains("adventure.php", ignoreCase = true)) return false
        val snarfblat = SNARFBLEAT_PATTERN.find(url)?.groupValues?.getOrNull(1) ?: return false
        return snarfblat in SBB_SNARFBLETS
    }

    private fun isSleazePlace(url: String?, html: String): Boolean {
        if (url.isNullOrBlank() || !url.contains("place.php", ignoreCase = true)) return false
        if (url.contains("whichplace=airport_sleaze", ignoreCase = true)) return true
        if (url.contains("whichplace=airport", ignoreCase = true) &&
            html.contains("whichplace=airport_sleaze", ignoreCase = true)
        ) {
            return true
        }
        return false
    }
}
