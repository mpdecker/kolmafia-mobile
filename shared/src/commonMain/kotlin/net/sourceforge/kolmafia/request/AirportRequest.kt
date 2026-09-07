package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.AirportRequest] Mystery Island / That 70s Volcano. */
object AirportRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("airport.php", ignoreCase = true)) return
        if (html.contains("You've already visited the airport today", ignoreCase = true) ||
            html.contains("You head to the airport", ignoreCase = true)
        ) {
            preferences?.setBoolean("_airportToday", true)
        }
    }

    fun registerRequest(url: String): Boolean = url.contains("airport.php", ignoreCase = true)
}
