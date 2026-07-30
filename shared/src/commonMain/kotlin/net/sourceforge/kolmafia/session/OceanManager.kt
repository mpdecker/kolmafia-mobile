package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.KoLConstants
import net.sourceforge.kolmafia.data.OceanDatabase
import net.sourceforge.kolmafia.data.OceanDatabase.OceanPoint
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.OceanRequest

/**
 * Pirate-ship ocean sailing automation from `oceanDestination`/`oceanAction` prefs.
 * Mirrors desktop [net.sourceforge.kolmafia.session.OceanManager].
 */
object OceanManager {

    sealed class OceanResult {
        data class Continued(val html: String, val url: String) : OceanResult()
        data class Stop(val message: String) : OceanResult()
        data class Manual(val message: String) : OceanResult()
    }

    private val urlLonPattern = Regex("""lon=(\d+)""")
    private val urlLatPattern = Regex("""lat=(\d+)""")

    fun shouldAutomate(preferences: Preferences): Boolean {
        val dest = preferences.getString("oceanDestination", "manual")
        return dest != "manual" && dest != "ignore"
    }

    fun getDestination(preferences: Preferences): OceanPoint? {
        val dest = preferences.getString("oceanDestination", "manual")
        return when (dest) {
            "manual", "ignore" -> null
            "muscle", "mysticality", "moxie", "sand", "altar", "sphere", "plinth" ->
                randomKeywordPoint(dest)
            "random" -> randomSafePoint()
            else -> if (dest.contains(",")) OceanPoint.parse(dest) else null
        }
    }

    suspend fun processOceanAdventure(
        oceanRequest: OceanRequest,
        preferences: Preferences,
        sessionLogger: SessionLogger? = null,
        log: (String) -> Unit = {},
    ): OceanResult {
        var destination = getDestination(preferences)
        if (destination == null) {
            return OceanResult.Manual("Pick a valid course.")
        }

        if (OceanDatabase.isMainland(destination)) {
            log("You cannot sail to the mainland.")
            destination = randomSafePoint()
            log("Random destination chosen: $destination")
        }

        val sailResult = oceanRequest.sail(destination.x, destination.y).getOrElse { e ->
            return OceanResult.Stop("Ocean sail failed: ${e.message}")
        }
        val (html, url) = sailResult
        registerRequest(url, sessionLogger)

        val action = preferences.getString("oceanAction", "savecontinue")
        val stop = action == "stop" || action == "savestop"
        val show = action == "show" || action == "saveshow"

        if (show) {
            log("Ocean sail response ready (show requested; no browser on mobile).")
        }

        return if (stop) {
            OceanResult.Stop("Stop")
        } else {
            OceanResult.Continued(html, url)
        }
    }

    fun registerRequest(urlString: String, sessionLogger: SessionLogger? = null) {
        if (!urlString.contains("ocean.php", ignoreCase = true)) return

        if (urlString.contains("intro=1")) {
            val message = "Encounter: Set an Open Course for the Virgin Booty"
            sessionLogger?.appendRawLine(message)
            return
        }

        val lonMatch = urlLonPattern.find(urlString) ?: return
        val latMatch = urlLatPattern.find(urlString) ?: return
        val lon = lonMatch.groupValues[1].toIntOrNull() ?: return
        val lat = latMatch.groupValues[1].toIntOrNull() ?: return
        if (!OceanPoint.valid(lon, lat)) return

        val point = OceanPoint(lon, lat)
        val dest = OceanDatabase.destinationAt(point)
        val destLabel = dest?.desc ?: "open ocean"
        sessionLogger?.appendRawLine("Setting sail for ($point) = $destLabel")
    }

    private fun randomKeywordPoint(keyword: String): OceanPoint? {
        val points = OceanDatabase.pointsForKeyword(keyword) ?: return null
        if (points.isEmpty()) return null
        return points[KoLConstants.RNG.nextInt(points.size)]
    }

    private fun randomSafePoint(): OceanPoint {
        while (true) {
            val lon = KoLConstants.RNG.nextInt(OceanPoint.X_MAX) + 1
            val lat = KoLConstants.RNG.nextInt(OceanPoint.Y_MAX) + 1
            val point = OceanPoint(lon, lat)
            if (OceanDatabase.isMainland(point)) continue
            if (OceanDatabase.destinationAt(point) == OceanDatabase.OceanDestination.PLINTH) continue
            return point
        }
    }
}
