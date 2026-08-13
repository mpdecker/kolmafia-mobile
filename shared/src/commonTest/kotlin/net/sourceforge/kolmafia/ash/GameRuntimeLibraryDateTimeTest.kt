package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertTrue

class GameRuntimeLibraryDateTimeTest {

    private val lib = GameRuntimeLibrary.forTesting()

    @Test
    fun todayToString_matchesDatePattern() {
        val result = outputLib(lib, "print(today_to_string());")
        assertTrue(result.matches(Regex("""\d{8}""")),
            "Expected YYYYMMDD but got: $result")
    }

    @Test
    fun nowToString_matchesDateTimePattern() {
        val result = outputLib(lib, "print(now_to_string());")
        assertTrue(result.matches(Regex("""\d{8} \d{2}:\d{2}:\d{2}""")),
            "Expected YYYYMMDD HH:mm:ss but got: $result")
    }

    @Test
    fun gamedayToString_isKoLCalendarDay() {
        val result = outputLib(lib, "print(gameday_to_string());")
        assertTrue(
            result.matches(
                Regex(
                    """(Jarlsuary|Frankruary|Starch|April|Martinus|Bill|Bor|Petember|Carlvember|Porktober|Boozember|Dougtember) [1-8]""",
                ),
            ),
            "Expected KoL calendar day but got: $result",
        )
    }

    @Test
    fun gamedayToInt_isInKoLYear() {
        val result = outputLib(lib, "print(to_string(gameday_to_int()));").toIntOrNull()
        assertTrue(result != null && result in 0..95, "gameday_to_int should be 0..95, got $result")
    }

    @Test
    fun timeToString_matchesTimePattern() {
        val result = outputLib(lib, "print(time_to_string());")
        assertTrue(result.matches(Regex("""\d{2}:\d{2}:\d{2}""")),
            "Expected HH:mm:ss but got: $result")
    }

    @Test
    fun rollover_nonnegativeWithNoCharacter() {
        val result = outputLib(lib, "print(to_string(rollover()));").toIntOrNull() ?: -1
        assertTrue(result >= 0, "rollover() should be >= 0 but got $result")
    }

    @Test
    fun moonPhase_returnsInt() {
        val result = outputLib(lib, "print(to_string(moon_phase()));").toIntOrNull()
        assertTrue(result != null, "moon_phase() should return parseable int")
    }
}
