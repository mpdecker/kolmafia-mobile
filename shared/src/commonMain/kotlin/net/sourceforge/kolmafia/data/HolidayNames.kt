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
}
