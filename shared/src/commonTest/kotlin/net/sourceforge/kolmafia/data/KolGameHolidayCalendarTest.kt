package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KolGameHolidayCalendarTest {

    @AfterTest
    fun tearDown() {
        KolGameHolidayCalendar.calendarDayOverride = null
    }

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

    @Test
    fun phaseStep_jarlsuary1_isNewMoons() {
        assertEquals(0, KolGameHolidayCalendar.phaseStep(0))
        assertEquals("new moon", KolGameHolidayCalendar.getRonaldPhaseAsString(0))
        assertEquals("new moon", KolGameHolidayCalendar.getGrimacePhaseAsString(0))
        assertEquals(
            "Moxie bonus today and yesterday.",
            KolGameHolidayCalendar.getMoonEffect(0),
        )
    }

    @Test
    fun phaseStep_calendarDay4_fullRonald() {
        assertEquals(4, KolGameHolidayCalendar.phaseStep(4))
        assertEquals("full moon", KolGameHolidayCalendar.getRonaldPhaseAsString(4))
        assertEquals("first quarter", KolGameHolidayCalendar.getGrimacePhaseAsString(4))
        assertEquals(
            "Mysticism bonus today (not tomorrow).",
            KolGameHolidayCalendar.getMoonEffect(4),
        )
    }

    @Test
    fun holidayPredictions_jarlsuary1_festivalToday() {
        val predictions = KolGameHolidayCalendar.getHolidayPredictions(0)
        assertTrue(predictions.any { it == "Festival of Jarlsberg: today" })
        assertTrue(predictions.any { it.startsWith("Valentine's Day:") })
    }

    @Test
    fun miniMoon_atCollision_frontGrimaceLeft() {
        assertEquals(0, KolGameHolidayCalendar.miniMoonPosition(KolGameHolidayCalendar.COLLISION_DAY_DIFFERENCE))
        assertEquals(
            "in front of Grimace, L side",
            KolGameHolidayCalendar.getMiniMoonAsString(KolGameHolidayCalendar.COLLISION_DAY_DIFFERENCE),
        )
    }

    @Test
    fun miniMoon_beforeCollision_unknown() {
        assertEquals(-1, KolGameHolidayCalendar.miniMoonPosition(0))
        assertEquals("unknown", KolGameHolidayCalendar.getMiniMoonAsString(0))
    }

    @Test
    fun getDayCountAsString_todayTomorrowDays() {
        assertEquals("today", KolGameHolidayCalendar.getDayCountAsString(0))
        assertEquals("tomorrow", KolGameHolidayCalendar.getDayCountAsString(1))
        assertEquals("11 days", KolGameHolidayCalendar.getDayCountAsString(11))
    }
}
