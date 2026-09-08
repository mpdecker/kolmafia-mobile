package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class HolidayCalendarTest {

    @Test
    fun halloween_october31() {
        assertEquals("Halloween", HolidayCalendar.getHoliday("20261031"))
    }

    @Test
    fun halloween_notEntireOctober() {
        assertEquals("", HolidayCalendar.getHoliday("20261015"))
    }

    @Test
    fun crimbo_december25() {
        assertEquals("Crimbo", HolidayCalendar.getHoliday("20261225"))
    }

    @Test
    fun festivalOfJarlsberg_january1() {
        assertEquals("Festival of Jarlsberg", HolidayCalendar.getHoliday("20260101"))
    }

    @Test
    fun stSneakyPetes_march17() {
        assertEquals("St. Sneaky Pete's Day", HolidayCalendar.getHoliday("20260317"))
    }

    @Test
    fun noHoliday_onOrdinaryDay() {
        assertEquals("", HolidayCalendar.getHoliday("20260305"))
    }
}
