package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KolGameHolidayCalendarTest {

    @Test
    fun calendarDayAsString_jarlsuary1() {
        assertEquals("Jarlsuary 1", KolGameHolidayCalendar.getCalendarDayAsString(0))
    }

    @Test
    fun calendarDayAsString_lastDayOfYear() {
        assertEquals("Dougtember 8", KolGameHolidayCalendar.getCalendarDayAsString(95))
    }

    @Test
    fun dayInKoLYear_inRange() {
        val day = KolGameHolidayCalendar.dayInKoLYear()
        assertTrue(day in 0..95, "dayInKoLYear=$day")
    }
}
