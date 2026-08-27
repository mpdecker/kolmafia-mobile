package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses campground HTML for workshed item and related craft gates.
 * Mirrors desktop [CampgroundRequest.parseWorkshed] gif detection (v1 subset).
 */
object CampgroundItemSync {

    const val ASDON_MARTIN_ID = 9508
    const val ASDON_MARTIN_FUEL_PREF = "asdonMartinFuel"
    const val CURRENT_WORKSHED_ITEM_ID_PREF = "_currentWorkshedItemId"
    const val CAMPGROUND_HAS_BURNING_LEAVES_PREF = "_campgroundHasBurningLeaves"
    const val CAMPGROUND_HAS_SOURCE_TERMINAL_PREF = "_campgroundHasSourceTerminal"
    const val CAMPGROUND_HAS_WITCHESS_SET_PREF = "_campgroundHasWitchessSet"

    private val FUEL_GAUGE_PATTERN =
        Regex("""fuel gauge reads ([\d,]+) litre""", RegexOption.IGNORE_CASE)

    internal val WORKSHED_GIF_TO_ID = listOf(
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

    fun hasWitchessSet(prefs: Preferences?): Boolean =
        prefs?.getBoolean(CAMPGROUND_HAS_WITCHESS_SET_PREF, false) == true

    fun asdonMartinFuel(prefs: Preferences?): Int =
        prefs?.getInt(ASDON_MARTIN_FUEL_PREF, 0) ?: 0

    fun syncFromHtml(html: String, prefs: Preferences?) {
        if (html.contains("burningleaves.gif", ignoreCase = true) ||
            html.contains("A Guide to Burning Leaves", ignoreCase = true)
        ) {
            prefs?.setBoolean(CAMPGROUND_HAS_BURNING_LEAVES_PREF, true)
        }
        if (html.contains("sourceterminal.gif", ignoreCase = true)) {
            prefs?.setBoolean(CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        }
        if (html.contains("chesstable.gif", ignoreCase = true)) {
            prefs?.setBoolean(CAMPGROUND_HAS_WITCHESS_SET_PREF, true)
        }
        syncKitchenFromHtml(html, prefs)
        syncAsdonFuelFromHtml(html, prefs)
        for ((gif, itemId) in WORKSHED_GIF_TO_ID) {
            if (html.contains(gif, ignoreCase = true)) {
                val previous = currentWorkshedItemId(prefs)
                prefs?.setInt(CURRENT_WORKSHED_ITEM_ID_PREF, itemId)
                if (previous >= 0 && previous != itemId) {
                    prefs?.setInt("_previousWorkshedItemId", previous)
                    prefs?.setBoolean("_workshedChanged", true)
                }
                break
            }
        }
        if (html.contains("Looks like the doctors are out for the day.", ignoreCase = true)) {
            prefs?.setInt(CURRENT_WORKSHED_ITEM_ID_PREF, 10815)
        }
        CampgroundInventorySync.syncFromHtml(html, prefs)
    }

    fun apply(
        preferences: Preferences?,
        html: String,
        url: String?,
        character: KoLCharacter? = null,
    ) {
        if (url == null || !url.contains("campground.php", ignoreCase = true)) return
        if (html.contains("action=bookshelf", ignoreCase = true)) {
            character?.setCampground(hasBookshelf = true)
        }
        syncFromHtml(html, preferences)
    }

    /** Desktop [CampgroundRequest] Asdon Martin fuel gauge parse (FUEL_PATTERN_1). */
    internal fun syncAsdonFuelFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        if (currentWorkshedItemId(prefs) != ASDON_MARTIN_ID &&
            !html.contains("asdongarage.gif", ignoreCase = true)
        ) {
            return
        }
        val match = FUEL_GAUGE_PATTERN.find(html) ?: return
        val fuel = match.groupValues[1].replace(",", "").toIntOrNull() ?: return
        prefs.setInt(ASDON_MARTIN_FUEL_PREF, fuel)
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
