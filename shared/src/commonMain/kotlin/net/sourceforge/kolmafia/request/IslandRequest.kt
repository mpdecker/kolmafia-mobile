package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.quest.IslandWarVisitSync
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.IslandRequest] wartime island hub. */
object IslandRequest {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
    ) {
        when {
            url.contains("bigisland.php", ignoreCase = true) ->
                IslandWarVisitSync.applyFromBigIslandVisit(url, html, preferences)
            url.contains("postwarisland.php", ignoreCase = true) ->
                IslandWarVisitSync.applyFromPostwarIslandVisit(url, html, preferences)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("bigisland.php", ignoreCase = true) ||
            url.contains("postwarisland.php", ignoreCase = true) ||
            url.contains("island.php", ignoreCase = true)
}
