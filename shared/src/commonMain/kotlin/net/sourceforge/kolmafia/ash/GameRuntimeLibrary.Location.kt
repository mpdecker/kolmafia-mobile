package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.location.LocationDatabase
import net.sourceforge.kolmafia.preferences.Preferences

internal fun GameRuntimeLibrary.registerLocationQueries(scope: AshScope) {
    regFn(scope, "my_location", AshType.LOCATION, emptyList()) { _, _ ->
        AshValue.location(preferences?.getString(Preferences.LAST_LOCATION, "") ?: "")
    }

    regFn(scope, "my_id", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.playerId ?: 0).toLong())
    }

    regFn(scope, "location_name", AshType.STRING, listOf("loc" to AshType.LOCATION)) { _, args ->
        AshValue.of(resolveLocationDisplayName(args[0].toString()))
    }

    regFn(scope, "location_name", AshType.STRING, listOf("loc" to AshType.STRING)) { _, args ->
        AshValue.of(resolveLocationDisplayName(args[0].toString()))
    }

    regFn(scope, "location_available", AshType.BOOLEAN, listOf("loc" to AshType.LOCATION)) { _, args ->
        val name = resolveLocationQueryName(args[0].toString())
        AshValue.of(AdventurePrep.canAdventureAtZone(name, character?.state?.value))
    }

    regFn(scope, "location_available", AshType.BOOLEAN, listOf("loc" to AshType.STRING)) { _, args ->
        val name = resolveLocationQueryName(args[0].toString())
        AshValue.of(AdventurePrep.canAdventureAtZone(name, character?.state?.value))
    }
}

internal fun GameRuntimeLibrary.resolveLocationDisplayName(loc: String): String {
    AdventureDatabase.getByName(loc)?.locationName?.let { return it }
    AdventureDatabase.getBySnarfblat(loc)?.locationName?.let { return it }
    LocationDatabase.findBySnarfblat(loc)?.name?.let { return it }
    resolveLocation(loc)?.name?.let { return it }
    return loc
}

internal fun GameRuntimeLibrary.resolveLocationQueryName(loc: String): String =
    AdventureDatabase.getByName(loc)?.locationName
        ?: AdventureDatabase.getBySnarfblat(loc)?.locationName
        ?: LocationDatabase.findBySnarfblat(loc)?.name
        ?: resolveLocation(loc)?.name
        ?: loc
