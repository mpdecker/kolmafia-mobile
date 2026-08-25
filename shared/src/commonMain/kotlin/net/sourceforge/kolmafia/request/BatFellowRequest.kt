package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BatManager
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [BatFellowRequest] thin place.php helper for Batfellow zones.
 */
object BatFellowRequest {

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("batman_") && !html.contains("batman_")) return false
        return BatManager.parsePlaceResponse(url, preferences)
    }

    fun registerRequest(url: String, preferences: Preferences?, sessionLogger: SessionLogger?): Boolean {
        if (!url.contains("batman_")) return false
        val zone = BatManager.placeToBatZone(url)
        val time = BatManager.getTimeLeftString()
        sessionLogger?.appendRawLine("[$time] Visiting $zone")
        preferences?.setString("batmanZone", zone)
        return true
    }
}
