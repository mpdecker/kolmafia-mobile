package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.ClanLoungeSwimmingPoolRequest] choice 585. */
object ClanLoungeSwimmingPoolRequest {
    const val HANDSTAND = 1
    const val GET_OUT = 2
    const val SAY = 3
    const val CLOSE_EYES = 4
    const val TREASURE = 5

    private val FOUND = Regex("""found a ([\w\-&; ]*)!""")

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("whichchoice=585", ignoreCase = true)) return
        if (!url.contains("action=treasure", ignoreCase = true)) return
        if (FOUND.containsMatchIn(html) || html.contains("found a", ignoreCase = true)) {
            preferences?.setBoolean("_olympicSwimmingPoolItemFound", true)
        }
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichchoice=585", ignoreCase = true)) return false
        val desc = when {
            url.contains("action=flip") -> "Doing handstand in clan VIP swimming pool"
            url.contains("action=leave") -> "Getting out of clan VIP swimming pool"
            url.contains("action=say") -> "Saying something in clan VIP swimming pool"
            url.contains("action=blink") -> "Blinking in clan VIP swimming pool"
            url.contains("action=treasure") -> "Diving for treasure in clan VIP swimming pool"
            else -> null
        }
        if (desc != null) sessionLogger?.appendRawLine(desc)
        return true
    }
}
