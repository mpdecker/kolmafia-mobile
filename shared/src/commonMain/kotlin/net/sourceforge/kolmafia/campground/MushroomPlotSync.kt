package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Detects mushroom plot ownership from knoll_mushrooms.php HTML and syncs
 * [lastMushroomPlot] pref. Mirrors desktop [MushroomManager.parsePlot] pref write.
 */
object MushroomPlotSync {

    fun hasPlot(html: String): Boolean =
        html.contains("<b>Your Mushroom Plot:</b>", ignoreCase = true)

    fun apply(preferences: Preferences?, character: KoLCharacter, html: String, url: String?) {
        if (url == null || !url.contains("knoll_mushrooms.php", ignoreCase = true)) return
        if (!hasPlot(html)) return
        preferences?.setInt("lastMushroomPlot", character.state.value.ascensionNumber)
    }
}
