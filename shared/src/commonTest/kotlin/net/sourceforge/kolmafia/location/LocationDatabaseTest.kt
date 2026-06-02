package net.sourceforge.kolmafia.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocationDatabaseTest {

    @Test
    fun search_byNameSubstring_returnMatchingZones() {
        val results = LocationDatabase.search("knob")
        assertTrue(results.isNotEmpty(), "Expected zones matching 'knob'")
        assertTrue(results.all { it.name.contains("knob", ignoreCase = true) ||
            it.zone.contains("knob", ignoreCase = true) })
    }

    @Test
    fun search_caseInsensitive() {
        val lower = LocationDatabase.search("forest")
        val upper = LocationDatabase.search("FOREST")
        assertEquals(lower.map { it.snarfblat }, upper.map { it.snarfblat })
    }

    @Test
    fun search_emptyQuery_returnsAll() {
        val all = LocationDatabase.search("")
        assertEquals(LocationDatabase.ALL_LOCATIONS.size, all.size)
    }

    @Test
    fun search_noMatch_returnsEmpty() {
        val results = LocationDatabase.search("zzznomatch")
        assertTrue(results.isEmpty())
    }

    @Test
    fun findBySnarfblat_returnsCorrectZone() {
        val zone = LocationDatabase.findBySnarfblat("20")
        assertNotNull(zone)
        assertTrue(zone.name.contains("Spooky Forest", ignoreCase = true))
    }

    @Test
    fun allLocations_haveUniqueSnarfblats() {
        val ids = LocationDatabase.ALL_LOCATIONS.map { it.snarfblat }
        assertEquals(ids.size, ids.toSet().size)
    }
}
