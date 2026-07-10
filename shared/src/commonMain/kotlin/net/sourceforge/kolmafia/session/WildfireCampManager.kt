package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks per-location Wildfire fire levels from captain HTML. Mirrors desktop [WildfireCampRequest].
 */
class WildfireCampManager(private val preferences: Preferences) {

    private var fireLevelsBySnarfblat: MutableMap<String, Int> = mutableMapOf()
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        val raw = preferences.getString(Preferences.WILDFIRE_FIRE_LEVELS)
        if (raw.isBlank()) {
            fireLevelsBySnarfblat = mutableMapOf()
            return
        }
        val parsed = mutableMapOf<String, Int>()
        for (entry in raw.split('|')) {
            if (entry.isBlank()) continue
            val eq = entry.indexOf('=')
            if (eq <= 0) continue
            val snarfblat = entry.substring(0, eq)
            val level = entry.substring(eq + 1).toIntOrNull() ?: continue
            parsed[snarfblat] = level
        }
        fireLevelsBySnarfblat = parsed
    }

    fun parseCaptain(html: String) {
        ensureLoaded()
        var changed = false
        for (match in CAPTAIN_ZONE.findAll(html)) {
            val snarfblat = match.groupValues[1]
            val level = match.groupValues[2].toIntOrNull() ?: continue
            if (fireLevelsBySnarfblat[snarfblat] != level) {
                fireLevelsBySnarfblat[snarfblat] = level
                changed = true
            }
        }
        if (changed) save()
    }

    fun getFireLevel(locationName: String): Int {
        ensureLoaded()
        val snarfblat = AdventureDatabase.getByName(locationName)?.snarfblat ?: return DEFAULT_FIRE_LEVEL
        return fireLevelsBySnarfblat[snarfblat] ?: DEFAULT_FIRE_LEVEL
    }

    internal fun setFireLevelForTest(locationName: String, level: Int) {
        ensureLoaded()
        val snarfblat = AdventureDatabase.getByName(locationName)?.snarfblat ?: return
        fireLevelsBySnarfblat[snarfblat] = level
        save()
    }

    private fun ensureLoaded() {
        if (!loaded) load()
    }

    private fun save() {
        val serialized = fireLevelsBySnarfblat.entries
            .sortedBy { it.key }
            .joinToString("|") { (snarfblat, level) -> "$snarfblat=$level" }
        preferences.setString(Preferences.WILDFIRE_FIRE_LEVELS, serialized)
    }

    companion object {
        const val DEFAULT_FIRE_LEVEL = 5

        private val CAPTAIN_ZONE = Regex(
            """<option.*?value="(\d+)">.*? \(.*?: (\d)\)</option>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
