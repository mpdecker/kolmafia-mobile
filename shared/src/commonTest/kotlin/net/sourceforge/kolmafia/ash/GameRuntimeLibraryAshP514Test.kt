package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.AdventureSpentTracker

class GameRuntimeLibraryAshP514Test {

    @Test
    fun revision_phase514() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun locations_listsVisitedTurns() {
        val tracker = AdventureSpentTracker(prefs())
        tracker.addTurn("The Haunted Pantry")
        tracker.addTurn("The Spooky Forest")
        tracker.addTurn("The Haunted Pantry")
        val lib = GameRuntimeLibrary(adventureSpentTracker = tracker)
        val out = outputLib(lib, """cli_execute("locations");""")
        assertTrue(out.contains("Visited Locations:"))
        assertTrue(out.contains("The Haunted Pantry (2)"))
        assertTrue(out.contains("The Spooky Forest (1)"))
    }

    @Test
    fun locations_empty_printsHeaderOnly() {
        val lib = GameRuntimeLibrary(adventureSpentTracker = AdventureSpentTracker(prefs()))
        val out = outputLib(lib, """cli_execute("locations");""")
        assertEquals("Visited Locations:", out.trim())
    }
}
