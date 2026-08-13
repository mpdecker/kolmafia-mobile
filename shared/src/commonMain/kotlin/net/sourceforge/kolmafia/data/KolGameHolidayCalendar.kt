package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.ash.kolRolloverDayDifference

/** Desktop [HolidayDatabase] game-calendar holidays for craft gates (SSPD, etc.). */
object KolGameHolidayCalendar {

    val MONTH_NAMES = listOf(
        "",
        "Jarlsuary",
        "Frankruary",
        "Starch",
        "April",
        "Martinus",
        "Bill",
        "Bor",
        "Petember",
        "Carlvember",
        "Porktober",
        "Boozember",
        "Dougtember",
    )

    private val GAME_HOLIDAYS = mapOf(
        1 to 1 to "Festival of Jarlsberg",
        2 to 4 to "Valentine's Day",
        3 to 3 to "St. Sneaky Pete's Day",
        4 to 2 to "Oyster Egg Day",
        5 to 2 to "El Dia De Los Muertos Borrachos",
        6 to 3 to "Generic Summer Holiday",
        7 to 4 to "Dependence Day",
        8 to 4 to "Arrrbor Day",
        9 to 6 to "Lab&oacute;r Day",
        10 to 8 to "Halloween",
        11 to 7 to "Feast of Boris",
        12 to 4 to "Yuletide",
    )

    fun dayInKoLYear(dayDifference: Long = kolRolloverDayDifference()): Int {
        val days = dayDifference.toInt()
        return ((days % 96) + 96) % 96
    }

    fun calendarComponents(calendarDay: Int = dayInKoLYear()): Pair<Int, Int> {
        val month = calendarDay / 8 % 12 + 1
        val day = calendarDay % 8 + 1
        return month to day
    }

    /** Desktop [HolidayDatabase.getCalendarDayAsString] — `"Jarlsuary 1"`. */
    fun getCalendarDayAsString(calendarDay: Int = dayInKoLYear()): String {
        val (month, day) = calendarComponents(calendarDay)
        val monthName = MONTH_NAMES.getOrNull(month) ?: "Jarlsuary"
        return "$monthName $day"
    }

    fun getHoliday(dayDifference: Long = kolRolloverDayDifference()): String {
        val holidays = getHolidays(dayDifference)
        return holidays.joinToString(" / ")
    }

    fun getHolidays(dayDifference: Long = kolRolloverDayDifference()): List<String> {
        val (month, day) = calendarComponents(dayInKoLYear(dayDifference))
        val holidays = mutableListOf<String>()
        GAME_HOLIDAYS[month to day]?.let { holidays.add(it) }
        if ("St. Sneaky Pete's Day" in holidays && "Feast of Boris" in holidays) {
            holidays.clear()
            holidays.add("Drunksgiving")
        } else if ("Feast of Boris" in holidays &&
            "El Dia De Los Muertos Borrachos" in holidays
        ) {
            holidays.clear()
            holidays.add("El Dia De Los Muertos Borrachos y Agradecido")
        }
        return holidays
    }

    fun contains(holidayName: String, dayDifference: Long = kolRolloverDayDifference()): Boolean =
        getHoliday(dayDifference).contains(holidayName)
}
