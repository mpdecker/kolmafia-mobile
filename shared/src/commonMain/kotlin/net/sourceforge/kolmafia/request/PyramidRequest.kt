package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.PyramidRequest] lower chamber unlock. */
object PyramidRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("pyramid.php", ignoreCase = true) &&
            !url.contains("whichplace=pyramid", ignoreCase = true)
        ) {
            return
        }
        if (html.contains("lower chamber", ignoreCase = true) &&
            html.contains("unlocked", ignoreCase = true)
        ) {
            preferences?.setBoolean("lowerChamberUnlock", true)
        }
        if (html.contains("middle chamber", ignoreCase = true) &&
            html.contains("unlocked", ignoreCase = true)
        ) {
            preferences?.setBoolean("middleChamberUnlock", true)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("pyramid.php", ignoreCase = true) ||
            url.contains("whichplace=pyramid", ignoreCase = true)
}
