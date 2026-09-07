package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BarrelShrineSync

/** Desktop [net.sourceforge.kolmafia.request.BarrelShrineRequest] da.php barrel shrine. */
object BarrelShrineRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (preferences == null) return
        if (url.contains("barrelshrine", ignoreCase = true) ||
            url.contains("whichplace=da", ignoreCase = true)
        ) {
            BarrelShrineSync.syncUnlockFromHtml(html, preferences)
        }
        if (url.contains("whichchoice=1100", ignoreCase = true)) {
            BarrelShrineSync.syncFromVisit(html, preferences)
            val option = Regex("""option=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            if (option != null) BarrelShrineSync.syncPostChoice(option, preferences)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("barrelshrine", ignoreCase = true) ||
            url.contains("whichchoice=1100", ignoreCase = true)
}
