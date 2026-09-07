package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DecorateTentChoiceSync
import net.sourceforge.kolmafia.session.ModeableChoiceSync

/** Desktop [net.sourceforge.kolmafia.request.DecorateTentRequest] choice 1392. */
object DecorateTentRequest {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int) -> Unit = {},
    ) {
        if (!url.contains("whichchoice=1392", ignoreCase = true)) return
        val option = Regex("""option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return
        DecorateTentChoiceSync.apply(
            choiceId = 1392,
            decision = option,
            html = html,
            preferences = preferences,
            consumeItem = consumeItem,
        )
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1392", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.LedCandleRequest] choice 1509. */
object LedCandleRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("whichchoice=1509", ignoreCase = true)) return
        ModeableChoiceSync.applyFromChoiceUrl(url, html, preferences)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1509", ignoreCase = true)
}
