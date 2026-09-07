package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.servant.EdServantManager

/** Desktop [net.sourceforge.kolmafia.request.EdBaseRequest]. */
object EdBaseRequest {
    fun parseResponse(url: String, html: String, edServantManager: EdServantManager?) {
        if (!url.contains("edbase", ignoreCase = true) &&
            !html.contains("whichchoice=1053")
        ) {
            return
        }
        if (html.contains("whichchoice=1053")) {
            edServantManager?.syncFromChoice1053(html)
        }
    }

    fun registerRequest(url: String): Boolean = url.contains("edbase", ignoreCase = true)
}
