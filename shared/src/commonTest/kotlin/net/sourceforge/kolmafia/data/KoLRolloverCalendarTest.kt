package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.ash.kolRolloverDayDifference

class KoLRolloverCalendarTest {

    @Test
    fun getDayDifference_isNonNegative() {
        assertTrue(KoLRolloverCalendar.getDayDifference() >= 0)
    }

    @Test
    fun kolRolloverDayDifference_matchesCalendarWrapper() {
        assertEquals(KoLRolloverCalendar.getDayDifference(), kolRolloverDayDifference())
    }
}
