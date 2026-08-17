package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar

class GameRuntimeLibraryAshP495Test {

    @AfterTest
    fun tearDown() {
        KolGameHolidayCalendar.calendarDayOverride = null
    }

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    @Test
    fun revision_phase495() {
        assertEquals("phase550", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun moon_jarlsuary1_printsPhasesAndFestival() {
        KolGameHolidayCalendar.calendarDayOverride = 0
        val listed = lines(outputLib(GameRuntimeLibrary(), """cli_execute("moon");"""))
        assertTrue(listed.contains("Jarlsuary 1"))
        assertTrue(listed.contains("Ronald: new moon"))
        assertTrue(listed.contains("Grimace: new moon"))
        assertTrue(listed.any { it.startsWith("Mini-moon:") })
        assertTrue(listed.contains("Festival of Jarlsberg: today"))
        assertTrue(listed.contains("Festival of Jarlsberg"))
        assertTrue(listed.contains("Moxie bonus today and yesterday."))
    }

    @Test
    fun moons_alias_printsCalendarDay() {
        KolGameHolidayCalendar.calendarDayOverride = 0
        val listed = lines(outputLib(GameRuntimeLibrary(), """cli_execute("moons");"""))
        assertTrue(listed.contains("Jarlsuary 1"))
        assertTrue(listed.contains("Ronald: new moon"))
    }

    @Test
    fun moon_calendarDay4_fullRonald() {
        KolGameHolidayCalendar.calendarDayOverride = 4
        val listed = lines(outputLib(GameRuntimeLibrary(), """cli_execute("moon");"""))
        assertTrue(listed.contains("Ronald: full moon"))
        assertTrue(listed.contains("Grimace: first quarter"))
        assertTrue(listed.contains("Mysticism bonus today (not tomorrow)."))
    }
}
