package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ModeableChoiceSync

/** Desktop [net.sourceforge.kolmafia.request.UmbrellaRequest] choice 1466. */
object UmbrellaRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("whichchoice=1466", ignoreCase = true) &&
            !url.contains("unbreakable+umbrella", ignoreCase = true)
        ) {
            return
        }
        ModeableChoiceSync.applyFromChoiceUrl(url, html, preferences)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1466", ignoreCase = true)
}
