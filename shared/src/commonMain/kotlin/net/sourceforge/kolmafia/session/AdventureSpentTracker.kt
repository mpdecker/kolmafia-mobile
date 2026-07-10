package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks per-location adventure turns spent this ascension.
 * Mirrors desktop [AdventureSpentDatabase].
 */
class AdventureSpentTracker(private val preferences: Preferences) {

    private var turnsByLocation: MutableMap<String, Int> = mutableMapOf()
    private var totalTrackedTurns: Int = 0
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        val raw = preferences.getString(Preferences.ADVENTURE_SPENT_TURNS)
        if (raw.isBlank()) {
            turnsByLocation = mutableMapOf()
            totalTrackedTurns = 0
            return
        }
        val parsed = mutableMapOf<String, Int>()
        for (entry in raw.split('|')) {
            if (entry.isBlank()) continue
            val eq = entry.indexOf('=')
            if (eq <= 0) continue
            val name = entry.substring(0, eq)
            val count = entry.substring(eq + 1).toIntOrNull() ?: continue
            parsed[name] = count
        }
        turnsByLocation = parsed
        totalTrackedTurns = parsed.values.sum()
    }

    fun addTurn(locationName: String) {
        ensureLoaded()
        if (locationName.isBlank()) return
        val turns = turnsByLocation.getOrDefault(locationName, 0) + 1
        turnsByLocation[locationName] = turns
        totalTrackedTurns++
        save()
    }

    fun getTurns(locationName: String): Int {
        ensureLoaded()
        return turnsByLocation.getOrDefault(locationName, 0)
    }

    fun getTotalTrackedTurns(): Int {
        ensureLoaded()
        return totalTrackedTurns
    }

    fun resetTurns() {
        turnsByLocation = mutableMapOf()
        totalTrackedTurns = 0
        loaded = true
        save()
    }

    /**
     * Records the turn count at which a noncombat was last seen in a force-NC zone.
     * Call before [addTurn] for the consuming turn (desktop sets pref at NC encounter time).
     */
    fun recordNoncombat(location: AdventureLocation) {
        ensureLoaded()
        val zone = AdventureDatabase.getByName(location.name) ?: return
        val forceNc = zone.forceNoncombat ?: 0
        if (forceNc <= 0) return
        val snarfblat = zone.snarfblat?.toIntOrNull() ?: return
        val prefKey = "lastNoncombat$snarfblat"
        preferences.setInt(prefKey, getTurns(location.name))
    }

    fun recordNoncombatIfNeeded(location: AdventureLocation, result: AdventureResult) {
        if (result is AdventureResult.NonCombat) {
            recordNoncombat(location)
        }
    }

    fun lastNoncombatTurnsSpent(zone: AdventureZone?): Long {
        val forceNc = zone?.forceNoncombat ?: 0
        if (forceNc <= 0) return -1L
        val snarfblat = zone?.snarfblat?.toIntOrNull() ?: return -1L
        val prefKey = "lastNoncombat$snarfblat"
        val stored = preferences.getInt(prefKey, -1)
        return if (stored < 0) -1L else stored.toLong()
    }

    fun turnsUntilForcedNoncombat(locationName: String): Long {
        val zone = AdventureDatabase.getByName(locationName) ?: return -1L
        val forceNc = zone.forceNoncombat ?: 0
        if (forceNc <= 0) return -1L
        val lastNc = lastNoncombatTurnsSpent(zone)
        if (lastNc < 0) return -1L
        val turnsSpent = getTurns(locationName).toLong()
        return maxOf(0L, forceNc - (turnsSpent - lastNc))
    }

    private fun ensureLoaded() {
        if (!loaded) load()
    }

    private fun save() {
        val serialized = turnsByLocation.entries
            .sortedBy { it.key.lowercase() }
            .joinToString("|") { (name, count) -> "$name=$count" }
        preferences.setString(Preferences.ADVENTURE_SPENT_TURNS, serialized)
    }
}
