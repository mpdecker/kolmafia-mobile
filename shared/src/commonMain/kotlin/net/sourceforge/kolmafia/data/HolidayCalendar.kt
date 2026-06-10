package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.ash.currentDateString

/**
 * Real-life holiday names for [holiday()] ASH/CLI.
 * Game-calendar holidays (Bill 1, etc.) are not included in this simplified port.
 */
object HolidayCalendar {

    fun getHoliday(dateYmd: String = currentDateString()): String {
        if (dateYmd.length != 8) return ""
        val month = dateYmd.substring(4, 6).toIntOrNull() ?: return ""
        val day = dateYmd.substring(6, 8).toIntOrNull() ?: return ""
        return realLifeHoliday(month, day) ?: ""
    }

    private fun realLifeHoliday(month: Int, day: Int): String? = when (month) {
        1 -> when (day) {
            1 -> "New Year's Day"
            else -> null
        }
        2 -> when (day) {
            2 -> "Groundhog Day"
            14 -> "Valentine's Day"
            else -> null
        }
        3 -> when (day) {
            17 -> "St. Patrick's Day"
            else -> null
        }
        4 -> when (day) {
            1 -> "April Fool's Day"
            else -> null
        }
        7 -> when (day) {
            4 -> "Dependence Day"
            else -> null
        }
        9 -> when (day) {
            19 -> "Talk Like a Pirate Day"
            else -> null
        }
        10 -> "Halloween"
        11 -> when (day) {
            1 -> "Mr. Accessory's Birthday"
            else -> thanksgivingDay(month, day)
        }
        12 -> "Yuletide"
        else -> null
    }

    /** US Thanksgiving — fourth Thursday of November. */
    private fun thanksgivingDay(month: Int, day: Int): String? {
        if (month != 11) return null
        var thursdays = 0
        for (d in 1..day) {
            if (dayOfWeek(month, d) == 4) thursdays++
        }
        return if (thursdays == 4 && dayOfWeek(month, day) == 4) "Feast of Boris" else null
    }

    /** 0=Sunday … 4=Thursday. Uses Zeller-style approximation on real calendar. */
    private fun dayOfWeek(month: Int, day: Int): Int {
        val y = currentDateString().substring(0, 4).toIntOrNull() ?: 2026
        val m = if (month < 3) month + 12 else month
        val yr = if (month < 3) y - 1 else y
        val k = yr % 100
        val j = yr / 100
        val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
        return (h + 6) % 7
    }
}
