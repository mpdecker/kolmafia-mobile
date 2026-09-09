package net.sourceforge.kolmafia.data

/**
 * Combined KoL game-calendar + real-life holidays for [holiday()] ASH/CLI,
 * matching desktop [HolidayDatabase.getHoliday].
 */
object HolidayNames {
    private var override: String? = null

    fun setHoliday(holiday: String) {
        override = holiday.trim().takeIf { it.isNotBlank() }
    }

    fun clearOverride() {
        override = null
    }

    fun getHoliday(): String = getHolidays().joinToString(" / ")

    fun getHolidays(replaceWithSpecial: Boolean = true): List<String> {
        val holidays = KolGameHolidayCalendar.getHolidays().toMutableList()
        HolidayCalendar.getHoliday().takeIf { it.isNotBlank() }?.let { holidays += it }
        if (replaceWithSpecial) {
            if ("St. Sneaky Pete's Day" in holidays && "Feast of Boris" in holidays) {
                holidays.clear()
                holidays.add("Drunksgiving")
            } else if ("Feast of Boris" in holidays &&
                "El Dia De Los Muertos Borrachos" in holidays
            ) {
                holidays.clear()
                holidays.add("El Dia De Los Muertos Borrachos y Agradecido")
            }
        }
        override?.let { holidays += it }
        return holidays.distinct()
    }

    /**
     * Desktop [HolidayDatabase.getEvents] — combined game + real-life holidays
     * **without** combo-day replacement, plus stat-day and Labor Day Eve.
     */
    fun getEvents(): List<String> {
        val list = getHolidays(replaceWithSpecial = false).toMutableList()
        val calendarDay = KolGameHolidayCalendar.dayInKoLYear()
        if (KolGameHolidayCalendar.getGameHolidayInDays(1, calendarDay) == "Lab&oacute;r Day") {
            list += "Lab&oacute;r Day Eve"
        }
        when (KolGameHolidayCalendar.getStatDay(calendarDay)) {
            "muscle" -> list += "Muscle Day"
            "mysticality" -> list += "Mysticality Day"
            "moxie" -> list += "Moxie Day"
        }
        return list
    }

    /**
     * Desktop [HolidayDatabase.getHolidaySummary] — combined holiday with
     * combo-day replacement for today, falling back to next game-calendar holiday.
     */
    fun getHolidaySummary(): String {
        val today = getHoliday()
        if (today.isNotBlank()) return KolGameHolidayCalendar.getDayCountAsString(0, today)
        val calendarDay = KolGameHolidayCalendar.dayInKoLYear()
        for (i in 0 until 96) {
            val holiday = KolGameHolidayCalendar.getGameHolidayInDays(i, calendarDay) ?: continue
            return KolGameHolidayCalendar.getDayCountAsString(i, holiday)
        }
        return ""
    }
}
