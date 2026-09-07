package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.clan.ClanManager

/** Desktop [net.sourceforge.kolmafia.request.ShowClanRequest] showclan.php join. */
object ShowClanRequest {
    fun parseResponse(url: String, html: String) {
        if (!url.contains("showclan.php", ignoreCase = true)) return
        if (!url.contains("action=joinclan", ignoreCase = true)) return
        if (html.contains("You have now changed your allegiance", ignoreCase = true)) {
            ClanManager.resetClanId()
        }
    }

    fun registerRequest(url: String): Boolean = url.contains("showclan.php", ignoreCase = true)
}
