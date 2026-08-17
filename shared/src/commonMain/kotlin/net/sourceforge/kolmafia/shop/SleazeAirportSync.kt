package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportSync

/**
 * Desktop [QuestManager] sleaze-airport visit detection + Spring Beach ticket use.
 * Elemental today-flags live in [AirportSync]; this remains for ticket-use callers.
 */
object SleazeAirportSync {

    const val PREF = "_sleazeAirportToday"
    const val SPRING_BEACH_TICKET = AirportSync.SPRING_BEACH_TICKET

    fun syncFromVisit(html: String, url: String?, prefs: Preferences) {
        AirportSync.syncFromVisit(html, url, prefs)
    }

    fun syncFromSpringBeachTicketUse(html: String, prefs: Preferences) {
        AirportSync.syncFromSpringBeachTicketUse(html, prefs)
    }
}
