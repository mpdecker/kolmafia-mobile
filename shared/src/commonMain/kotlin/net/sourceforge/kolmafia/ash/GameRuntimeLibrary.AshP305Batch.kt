package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.FloundryDatabase

/** AshP305 — floundry location map: `get_fishing_locations`. */
internal fun GameRuntimeLibrary.registerAshP305Batch(scope: AshScope) {
    val stringLocationType = AggregateType(AshType.STRING, AshType.LOCATION)

    regFn(scope, "get_fishing_locations", stringLocationType, emptyList()) { _, _ ->
        val result = AggregateValue(stringLocationType)
        val prefs = preferences ?: return@regFn result
        for (entry in FloundryDatabase.allItems()) {
            val fish = entry.fish
            val prefKey = FloundryDatabase.locationPrefForFish(fish) ?: continue
            val locationName = prefs.getString(prefKey, "")
            if (locationName.isEmpty()) continue
            val resolved = AdventureDatabase.getByName(locationName)?.locationName ?: locationName
            result[AshValue.of(fish)] = AshValue.location(resolved)
        }
        result
    }
}
