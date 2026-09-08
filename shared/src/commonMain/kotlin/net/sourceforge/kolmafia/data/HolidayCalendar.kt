package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.ash.currentDateString

/**
 * Real-life holiday names for [holiday()] ASH/CLI.
 * Mirrors desktop [HolidayDatabase.getRealLifeHoliday] / [getRealLifeOnlyHoliday].
 */
object HolidayCalendar {

    fun getHoliday(dateYmd: String = currentDateString()): String {
        if (dateYmd.length != 8) return ""
        val year = dateYmd.substring(0, 4).toIntOrNull() ?: return ""
        val month = dateYmd.substring(4, 6).toIntOrNull() ?: return ""
        val day = dateYmd.substring(6, 8).toIntOrNull() ?: return ""
        return realLifeHoliday(year, month, day) ?: ""
    }

    /** Desktop HolidayDatabase.isMonday — real-calendar Monday gate for lasagna garish bonus. */
    fun isMonday(dateYmd: String = currentDateString()): Boolean {
        if (dateYmd.length != 8) return false
        val year = dateYmd.substring(0, 4).toIntOrNull() ?: return false
        val month = dateYmd.substring(4, 6).toIntOrNull() ?: return false
        val day = dateYmd.substring(6, 8).toIntOrNull() ?: return false
        return dayOfWeek(year, month, day) == 1
    }

    private fun realLifeHoliday(year: Int, month: Int, day: Int): String? {
        // Primary real-life holidays (also shown alongside game calendar).
        when {
            month == 1 && day == 1 -> return "Festival of Jarlsberg"
            month == 2 && day == 14 -> return "Valentine's Day"
            month == 3 && day == 17 -> return "St. Sneaky Pete's Day"
            month == 7 && day == 4 -> return "Dependence Day"
            month == 10 && day == 31 -> return "Halloween"
            isEaster(year, month, day) -> return "Oyster Egg Day"
            isThanksgiving(year, month, day) -> return "Feast of Boris"
        }
        // Real-life-only holidays.
        return when {
            month == 12 && day == 15 -> {
                val nth = year - 2004
                "KoLmafia's ${withOrdinalSuffix(nth)} Birthday"
            }
            month == 2 && day == 2 -> "Groundhog Day"
            month == 4 && day == 1 -> "April Fool's Day"
            month == 9 && day == 19 -> "Talk Like a Pirate Day"
            month == 12 && day == 25 -> "Crimbo"
            month == 10 && day == 22 -> "Holatuwol's Birthday"
            month == 9 && day == 23 -> "Veracity's Birthday"
            month == 2 && day == 17 -> "Gausie's Birthday"
            month == 11 && day == 1 -> "Mr. Accessory's Birthday"
            else -> null
        }
    }

    /** US Thanksgiving — fourth Thursday of November. */
    private fun isThanksgiving(year: Int, month: Int, day: Int): Boolean {
        if (month != 11) return false
        var thursdays = 0
        for (d in 1..day) {
            if (dayOfWeek(year, month, d) == 4) thursdays++
        }
        return thursdays == 4 && dayOfWeek(year, month, day) == 4
    }

    /**
     * Anonymous Gregorian algorithm for Easter Sunday (desktop HolidayDatabase.getEaster).
     */
    private fun isEaster(year: Int, month: Int, day: Int): Boolean {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val easterMonth = (h + l - 7 * m + 114) / 31
        val easterDay = ((h + l - 7 * m + 114) % 31) + 1
        return month == easterMonth && day == easterDay
    }

    private fun withOrdinalSuffix(n: Int): String {
        val mod100 = n % 100
        val suffix = when {
            mod100 in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    /** 0=Sunday … 6=Saturday. Uses Zeller congruence. */
    private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
        val m = if (month < 3) month + 12 else month
        val yr = if (month < 3) year - 1 else year
        val k = yr % 100
        val j = yr / 100
        val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
        return (h + 6) % 7
    }
}
