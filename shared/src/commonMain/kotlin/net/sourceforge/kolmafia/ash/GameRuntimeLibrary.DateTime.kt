package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.HolidayNames
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar

internal fun GameRuntimeLibrary.registerDateTimeQueries(scope: AshScope) {

    regFn(scope, "today_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(currentDateString())
    }

    regFn(scope, "now_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(currentDateTimeString())
    }

    regFn(scope, "time_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(currentTimeOfDayString())
    }

    regFn(scope, "gameday_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(KolGameHolidayCalendar.getCalendarDayAsString())
    }

    regFn(scope, "gameday_to_int", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(KolGameHolidayCalendar.dayInKoLYear().toLong())
    }

    regFn(scope, "holiday", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(HolidayNames.getHoliday())
    }

    regFn(scope, "rollover", AshType.INT, emptyList()) { _, _ ->
        val secs = character?.state?.value?.secondsUntilRollover ?: 0L
        AshValue.of(secs.coerceAtLeast(0L))
    }

    regFn(scope, "moon_phase", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.moonPhase ?: 0).toLong())
    }
}
