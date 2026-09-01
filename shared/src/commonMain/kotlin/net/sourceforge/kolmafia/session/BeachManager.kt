package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Canonical headless Beach Comb state boundary.
 *
 * [BeachCombManager] remains as a compatibility implementation for older
 * callers; new request and hook code should use this facade.
 */
object BeachManager {
    const val CHOICE_ID = 1388
    const val FREE_WALKS_PER_DAY = 11

    val beachHeads: Array<BeachHeadAvailability.BeachHead>
        get() = BeachHeadAvailability.BEACH_HEADS

    fun getBeachHeadPreference(property: String, preferences: Preferences?): Set<Int> =
        if (property == BeachHeadAvailability.HEADS_USED_PREF) {
            BeachHeadAvailability.parseBeachHeadsUsed(preferences)
        } else {
            BeachHeadAvailability.parseBeachHeadsUnlocked(preferences)
        }

    fun setBeachHeadPreference(
        property: String,
        values: Set<Int>,
        preferences: Preferences?,
    ) {
        if (preferences == null) return
        preferences.setString(property, values.sorted().joinToString(","))
    }

    fun stringToLayout(input: String): Map<Int, String> =
        BeachCombManager.stringToLayout(input)

    fun layoutToString(layout: Map<Int, String>): String =
        BeachCombManager.layoutToString(layout)

    fun parseCombUsage(html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        return BeachCombManager.parseCombUsage(html, prefs)
    }

    fun parseBeachHeadCombing(html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        return BeachCombManager.parseBeachHeadCombing(html, prefs)
    }

    fun parseBeachMap(
        html: String,
        preferences: Preferences?,
        log: ((String) -> Unit)? = null,
    ): Boolean {
        val prefs = preferences ?: return false
        return BeachCombManager.parseBeachMap(html, prefs, log)
    }

    fun markCombedSquare(url: String, html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        return BeachCombManager.markCombedSquare(url, html, prefs)
    }

    fun freeWalksUsed(preferences: Preferences?): Int =
        preferences?.getInt("_freeBeachWalksUsed", 0)?.coerceIn(0, FREE_WALKS_PER_DAY) ?: 0

    fun actionConsumesAdventure(option: Int, preferences: Preferences?): Boolean =
        option in setOf(1, 2, 3, 4) && freeWalksUsed(preferences) >= FREE_WALKS_PER_DAY
}
