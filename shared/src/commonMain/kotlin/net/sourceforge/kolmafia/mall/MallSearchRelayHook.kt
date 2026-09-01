package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.preferences.Preferences

/** Wire MallSearchDecorator when relayActive pref is true — headless relay parity. */
object MallSearchRelayHook {

    fun maybeDecorate(
        html: String,
        preferences: Preferences?,
        passwordHash: String = "",
        playerId: Int = 0,
    ): String {
        if (preferences?.getBoolean("relayActive", false) != true) return html
        if (!html.contains("mall.php", ignoreCase = true) &&
            !html.contains("graybelow", ignoreCase = true)
        ) {
            return html
        }
        val preprocessed = MallSearchHtmlPreprocessor.preprocess(html)
        val ownStoreIds = if (playerId > 0) setOf(playerId) else emptySet()
        return MallSearchDecorator.decorateMallSearch(preprocessed, passwordHash, ownStoreIds)
    }

    fun isMallSearchHtml(html: String): Boolean =
        html.contains("searchmall=1", ignoreCase = true) ||
            html.contains("class=\"graybelow", ignoreCase = true) ||
            html.contains("whichstore=", ignoreCase = true)
}
