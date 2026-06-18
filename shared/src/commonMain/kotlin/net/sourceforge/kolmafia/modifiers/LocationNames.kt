package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.location.LocationDatabase

/**
 * ASH location name resolution. Canonical display name from [LocationDatabase] and [AdventureDatabase].
 */
object LocationNames {

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        LocationDatabase.ALL_LOCATIONS.find { it.name.equals(trimmed, ignoreCase = true) }?.let {
            return it.name
        }
        LocationDatabase.findBySnarfblat(trimmed)?.let { return it.name }
        AdventureDatabase.getByName(trimmed.lowercase())?.locationName?.let { return it }
        AdventureDatabase.getBySnarfblat(trimmed)?.locationName?.let { return it }

        val locationMatches = LocationDatabase.ALL_LOCATIONS.filter {
            it.name.contains(trimmed, ignoreCase = true)
        }
        if (locationMatches.size == 1) return locationMatches[0].name

        val adventureMatches = AdventureDatabase.search(trimmed)
        if (adventureMatches.size == 1) return adventureMatches[0].locationName

        if (trimmed.all { it.isDigit() }) {
            return LocationDatabase.findBySnarfblat(trimmed)?.name ?: trimmed
        }
        return null
    }

    fun isValid(name: String): Boolean = resolve(name) != null
}
