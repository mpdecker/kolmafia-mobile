package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SeaMerkinSync
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.SeaMerkinRequest] temple/colosseum visit hub. */
object SeaMerkinRequest {
    fun parseResponse(
        url: String,
        html: String,
        inSeaPath: Boolean,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        if (!url.contains("sea_merkin.php", ignoreCase = true)) return
        SeaMerkinSync.parseTemple(url, html, inSeaPath, preferences, sessionLogger)
    }

    fun parseColosseum(
        url: String,
        html: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        SeaMerkinSync.parseColosseum(url, html, preferences, sessionLogger)
    }

    /** Desktop registerRequest — temple visits defer to adventure routing. */
    fun registerRequest(url: String): Boolean {
        if (!url.startsWith("sea_merkin.php", ignoreCase = true)) return false
        return false
    }
}
