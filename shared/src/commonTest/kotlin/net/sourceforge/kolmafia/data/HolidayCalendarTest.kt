package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class HolidayCalendarTest {

    @Test
    fun halloween_entireOctober() {
        assertEquals("Halloween", HolidayCalendar.getHoliday("20261015"))
    }

    @Test
    fun yuletide_december() {
        assertEquals("Yuletide", HolidayCalendar.getHoliday("20261225"))
    }

    @Test
    fun noHoliday_onOrdinaryDay() {
        assertEquals("", HolidayCalendar.getHoliday("20260305"))
    }
}
