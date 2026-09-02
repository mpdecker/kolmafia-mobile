package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class GameRuntimeLibraryAshP497Test {

    @AfterTest
    fun tearDown() {
        KolGameHolidayCalendar.calendarDayOverride = null
    }

    private fun echoLib(): Pair<GameRuntimeLibrary, SessionLogger> {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        return GameRuntimeLibrary(sessionLogger = logger) to logger
    }

    @Test
    fun revision_phase497() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun logecho_writesSessionLogOnly() {
        val (lib, logger) = echoLib()
        val out = outputLib(lib, """cli_execute("logecho hello");""")
        assertFalse(out.contains("hello"))
        assertTrue(logger.recentLines().any { it == " > hello" })
    }

    @Test
    fun logprint_alias_writesSessionLog() {
        val (lib, logger) = echoLib()
        outputLib(lib, """cli_execute("logprint world");""")
        assertTrue(logger.recentLines().any { it == " > world" })
    }

    @Test
    fun fecho_printsAndLogs() {
        val (lib, logger) = echoLib()
        val out = outputLib(lib, """cli_execute("fecho hello");""")
        assertEquals("hello", out)
        assertTrue(logger.recentLines().any { it == " > hello" })
    }

    @Test
    fun fprint_alias_printsAndLogs() {
        val (lib, logger) = echoLib()
        val out = outputLib(lib, """cli_execute("fprint hi");""")
        assertEquals("hi", out)
        assertTrue(logger.recentLines().any { it == " > hi" })
    }

    @Test
    fun logecho_timestamp_logsCalendarDay() {
        KolGameHolidayCalendar.calendarDayOverride = 0
        val (lib, logger) = echoLib()
        val out = outputLib(lib, """cli_execute("logecho timestamp");""")
        assertEquals("", out)
        assertTrue(logger.recentLines().any { it == " > Jarlsuary 1" })
    }

    @Test
    fun logecho_escapesLtAndStripsNewlines() {
        val (lib, logger) = echoLib()
        outputLib(lib, """cli_execute("logecho a<b");""")
        assertTrue(logger.recentLines().any { it == " > a&lt;b" })
    }
}
