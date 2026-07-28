package net.sourceforge.kolmafia.clan

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses clan VIP lounge HTML for lounge item availability.
 * Mirrors desktop [ClanLoungeRequest.findImage] for the floundry and photo booth.
 */
object ClanLoungeSync {

    const val CLAN_HAS_FLOUNDRY_PREF = "_clanHasFloundry"
    const val CLAN_HAS_PHOTO_BOOTH_PREF = "_clanHasPhotoBooth"

    fun hasFloundry(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CLAN_HAS_FLOUNDRY_PREF, false) == true

    fun hasPhotoBooth(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CLAN_HAS_PHOTO_BOOTH_PREF, false) == true

    fun syncFromHtml(html: String, prefs: Preferences?) {
        prefs?.setBoolean(CLAN_HAS_FLOUNDRY_PREF, html.contains("vipfloundry.gif", ignoreCase = true))
        prefs?.setBoolean(CLAN_HAS_PHOTO_BOOTH_PREF, html.contains("photobooth.gif", ignoreCase = true))
    }

    fun apply(preferences: Preferences?, html: String, url: String?) {
        if (url == null || !url.contains("clan_viplounge.php", ignoreCase = true)) return
        syncFromHtml(html, preferences)
    }
}
