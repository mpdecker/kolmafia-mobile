package net.sourceforge.kolmafia.campground

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DwellingSyncTest {

    @Test
    fun parseDwellingCode_numericAndAlpha() {
        assertEquals(4, DwellingSync.parseDwellingCode("4"))
        assertEquals(10, DwellingSync.parseDwellingCode("a"))
        assertEquals(17, DwellingSync.parseDwellingCode("h"))
    }

    @Test
    fun currentDwellingItemId_defaultsToBigRock() {
        assertEquals(DwellingSync.BIG_ROCK_ITEM_ID, DwellingSync.currentDwellingItemId(null))
    }

    @Test
    fun parseDwellingCode_emptyReturnsNull() {
        assertNull(DwellingSync.parseDwellingCode(""))
    }
}
