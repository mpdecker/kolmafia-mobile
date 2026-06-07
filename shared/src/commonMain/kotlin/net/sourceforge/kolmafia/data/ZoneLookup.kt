package net.sourceforge.kolmafia.data

/**
 * Minimal interface for zone monster lookups used by AdventureManager's pre-flight check.
 * CombatDatabase implements this interface, and tests can supply a stub.
 */
interface ZoneLookup {
    fun getByLocation(name: String): ZoneCombatData?
}
