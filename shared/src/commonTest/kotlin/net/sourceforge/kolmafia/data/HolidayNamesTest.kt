package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HolidayNamesTest {

    @AfterTest
    fun tearDown() {
        HolidayNames.clearOverride()
        KolGameHolidayCalendar.calendarDayOverride = null
    }

    @Test
    fun override_appendedToHolidayString() {
        HolidayNames.setHoliday("Bill 1")
        assertTrue(HolidayNames.getHoliday().contains("Bill 1"))
    }

    @Test
    fun getHolidays_comboDay_drunksgiving() {
        // Force calendarDay to St. Sneaky Pete's Day (Starch 3 = calendarDay 18)
        // and set a real-life holiday that returns "Feast of Boris" (Thanksgiving).
        // Since we can't override HolidayCalendar date, test game-only combo:
        // St. Sneaky Pete's Day is month 3 day 3 = calendarDay 18
        // Feast of Boris is month 11 day 7 = calendarDay 86
        // These never share a game-calendar day, so combo only happens with real-life overlay.
        // Test the replaceWithSpecial=false bypass instead.
        KolGameHolidayCalendar.calendarDayOverride = 18 // St. Sneaky Pete's Day
        val noReplace = HolidayNames.getHolidays(replaceWithSpecial = false)
        assertTrue(noReplace.any { it == "St. Sneaky Pete's Day" })
    }

    @Test
    fun getEvents_includesStatDay() {
        // calendarDay 8 = Muscle Day (8 % 16 == 8)
        KolGameHolidayCalendar.calendarDayOverride = 8
        val events = HolidayNames.getEvents()
        assertTrue(events.any { it == "Muscle Day" })
    }

    @Test
    fun getEvents_noComboReplacement() {
        // getEvents uses replaceWithSpecial=false, so combo names should NOT appear
        val events = HolidayNames.getEvents()
        assertFalse(events.any { it == "Drunksgiving" })
    }

    @Test
    fun getHolidaySummary_todayWhenHoliday() {
        // calendarDay 0 = Festival of Jarlsberg
        KolGameHolidayCalendar.calendarDayOverride = 0
        val summary = HolidayNames.getHolidaySummary()
        assertTrue(summary.contains("today"), "Expected 'today' in summary='$summary'")
    }

    @Test
    fun getHolidaySummary_futureCountdown() {
        // calendarDay 1 = no holiday; nearest is Valentine's Day at Frankruary 4 = calendarDay 11
        KolGameHolidayCalendar.calendarDayOverride = 1
        val summary = HolidayNames.getHolidaySummary()
        assertTrue(summary.contains("days") || summary.contains("tomorrow"),
            "Expected countdown in summary='$summary'")
    }
}
