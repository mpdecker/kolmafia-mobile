package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses campground HTML for workshed item and related craft gates.
 * Mirrors desktop [CampgroundRequest.parseWorkshed] gif detection (v1 subset).
 */
object CampgroundItemSync {

    const val CURRENT_WORKSHED_ITEM_ID_PREF = "_currentWorkshedItemId"
    const val CAMPGROUND_HAS_BURNING_LEAVES_PREF = "_campgroundHasBurningLeaves"
    const val CAMPGROUND_HAS_SOURCE_TERMINAL_PREF = "_campgroundHasSourceTerminal"

    private val WORKSHED_GIF_TO_ID = listOf(
        "wbchemset.gif" to 6967,
        "wboven.gif" to 6966,
        "wblprom.gif" to 7037,
        "wbstill.gif" to 7036,
        "wbanvil.gif" to 6965,
        "wbdrillpress.gif" to 6964,
        "snowmachine.gif" to 7082,
        "spinningwheel.gif" to 7140,
        "genelab.gif" to 7382,
        "asdongarage.gif" to 9508,
        "horadricoven.gif" to 10335,
        "cmcabinet.gif" to 10815,
    )

    fun currentWorkshedItemId(prefs: Preferences?): Int =
        prefs?.getInt(CURRENT_WORKSHED_ITEM_ID_PREF, -1) ?: -1

    fun hasWorkshedItem(prefs: Preferences?, itemId: Int): Boolean =
        currentWorkshedItemId(prefs) == itemId

    fun hasBurningLeaves(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CAMPGROUND_HAS_BURNING_LEAVES_PREF, false) == true

    fun hasSourceTerminal(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, false) == true

    fun syncFromHtml(html: String, prefs: Preferences?) {
        if (html.contains("burningleaves.gif", ignoreCase = true) ||
            html.contains("A Guide to Burning Leaves", ignoreCase = true)
        ) {
            prefs?.setBoolean(CAMPGROUND_HAS_BURNING_LEAVES_PREF, true)
        }
        if (html.contains("sourceterminal.gif", ignoreCase = true)) {
            prefs?.setBoolean(CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        }
        syncKitchenFromHtml(html, prefs)
        for ((gif, itemId) in WORKSHED_GIF_TO_ID) {
            if (html.contains(gif, ignoreCase = true)) {
                prefs?.setInt(CURRENT_WORKSHED_ITEM_ID_PREF, itemId)
                return
            }
        }
        if (html.contains("Looks like the doctors are out for the day.", ignoreCase = true)) {
            prefs?.setInt(CURRENT_WORKSHED_ITEM_ID_PREF, 10815)
        }
    }

    fun apply(preferences: Preferences?, html: String, url: String?) {
        if (url == null || !url.contains("campground.php", ignoreCase = true)) return
        syncFromHtml(html, preferences)
    }

    /** Desktop [CampgroundRequest.parseKitchen] gif detection. */
    internal fun syncKitchenFromHtml(html: String, prefs: Preferences?) {
        if (html.contains("ezcook.gif", ignoreCase = true)) {
            prefs?.setBoolean("hasOven", true)
        }
        if (html.contains("oven.gif", ignoreCase = true)) {
            prefs?.setBoolean("hasRange", true)
        }
        if (html.contains("shaker.gif", ignoreCase = true)) {
            prefs?.setBoolean("hasShaker", true)
        }
        if (html.contains("cocktailkit.gif", ignoreCase = true)) {
            prefs?.setBoolean("hasCocktailKit", true)
        }
        if (html.contains("chefinbox.gif", ignoreCase = true) ||
            html.contains("cchefbox.gif", ignoreCase = true)
        ) {
            prefs?.setBoolean("hasChef", true)
        }
        if (html.contains("bartinbox.gif", ignoreCase = true) ||
            html.contains("cbartbox.gif", ignoreCase = true)
        ) {
            prefs?.setBoolean("hasBartender", true)
        }
    }
}
