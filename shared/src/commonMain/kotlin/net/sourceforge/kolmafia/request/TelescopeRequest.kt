package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.TelescopeRequest]. */
object TelescopeRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("telescope", ignoreCase = true) &&
            !url.contains("campground.php", ignoreCase = true)
        ) {
            return
        }
        if (!url.contains("telescope", ignoreCase = true) &&
            !html.contains("telescope", ignoreCase = true)
        ) {
            return
        }
        Regex("""You've looked through your telescope (\d+) times""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.let { preferences?.setInt("telescopeUpgrades", it) }
        if (html.contains("high", ignoreCase = true) && url.contains("action=telscope", ignoreCase = true)) {
            preferences?.setBoolean("telescopeLookedHigh", true)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("telescope", ignoreCase = true)
}
