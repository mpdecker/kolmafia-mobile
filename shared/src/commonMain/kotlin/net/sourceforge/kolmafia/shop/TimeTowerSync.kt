package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [QuestManager.handleTimeTower] pref sync + storage cache invalidation on toggle. */
object TimeTowerSync {

    const val PREF = "timeTowerAvailable"

    val CHRONER_SHOP_IDS = setOf(
        "applestore",
        "caveshop",
        "conmerch",
        "nina",
        "shakeshop",
        "shoeshop",
        "twitchsoup",
        "twitch_alliedhq",
    )

    private const val SHOP_GONE_MARKER = "That store isn't there anymore."
    private const val TWITCH_GONE_MARKER = "temporal ether"

    fun syncFromChronerShopHtml(html: String, prefs: Preferences) {
        setAvailable(!html.contains(SHOP_GONE_MARKER, ignoreCase = true), prefs)
    }

    fun syncFromTwitchPlaceHtml(html: String, prefs: Preferences) {
        setAvailable(!html.contains(TWITCH_GONE_MARKER, ignoreCase = true), prefs)
    }

    private fun setAvailable(available: Boolean, prefs: Preferences) {
        if (prefs.getBoolean(PREF, false) == available) return
        prefs.setBoolean(PREF, available)
        prefs.setString(Preferences.CACHED_STORAGE, "")
        prefs.setString(Preferences.CACHED_FREEPULLS, "")
    }
}
