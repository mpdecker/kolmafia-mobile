package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.ash.kolRolloverDayDifference

/** Desktop [HolidayDatabase] game-calendar holidays for craft gates (SSPD, etc.). */
object KolGameHolidayCalendar {

    /** KoL day-difference of 2006-06-03 collision (NEWYEAR 2005-09-17, White Wednesday −1). */
    const val COLLISION_DAY_DIFFERENCE = 258L

    internal var calendarDayOverride: Int? = null

    private val PHASE_NAMES = listOf(
        "new moon",
        "waxing crescent",
        "first quarter",
        "waxing gibbous",
        "full moon",
        "waning gibbous",
        "third quarter",
        "waning crescent",
    )

    private val MINI_MOON_NAMES = listOf(
        "in front of Grimace, L side",
        "in front of Grimace, R side",
        "far right",
        "behind Grimace",
        "in back, near Grimace",
        "in back, near Ronald",
        "behind Ronald",
        "far left",
        "in front of Ronald, L side",
        "in front of Ronald, R side",
        "front center",
    )

    private val STAT_EFFECT = listOf(
        "Moxie bonus today and yesterday.",
        "3 days until Mysticism.",
        "2 days until Mysticism.",
        "Mysticism bonus tomorrow (not today).",
        "Mysticism bonus today (not tomorrow).",
        "3 days until Muscle.",
        "2 days until Muscle.",
        "Muscle bonus tomorrow (not today).",
        "Muscle bonus today and tomorrow.",
        "Muscle bonus today and yesterday.",
        "2 days until Mysticism.",
        "Mysticism bonus tomorrow (not today).",
        "Mysticism bonus today (not tomorrow).",
        "2 days until Moxie.",
        "Moxie bonus tomorrow (not today).",
        "Moxie bonus today and tomorrow.",
    )

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
        calendarDayOverride?.let { return it }
        val days = dayDifference.toInt()
        return ((days % 96) + 96) % 96
    }

    /** Desktop [HolidayDatabase.guessPhaseStep] — `(calendarDay + 16) % 16`. */
    fun phaseStep(calendarDay: Int = dayInKoLYear()): Int = (calendarDay + 16) % 16

    fun ronaldPhaseIndex(calendarDay: Int = dayInKoLYear()): Int = phaseStep(calendarDay) % 8

    fun grimacePhaseIndex(calendarDay: Int = dayInKoLYear()): Int = phaseStep(calendarDay) / 2

    /** Desktop [HolidayDatabase.getRonaldMoonlight]. */
    fun ronaldMoonlight(calendarDay: Int = dayInKoLYear()): Int {
        val phase = ronaldPhaseIndex(calendarDay)
        return if (phase > 4) 8 - phase else phase
    }

    /** Desktop [HolidayDatabase.getGrimaceMoonlight]. */
    fun grimaceMoonlight(calendarDay: Int = dayInKoLYear()): Int {
        val phase = grimacePhaseIndex(calendarDay)
        return if (phase > 4) 8 - phase else phase
    }

    /** Desktop [HolidayDatabase.getMoonlight] — Ronald + Grimace + Hamburglar. */
    fun getMoonlight(calendarDay: Int = dayInKoLYear(), dayDifference: Long = kolRolloverDayDifference()): Int {
        val ronald = ronaldPhaseIndex(calendarDay)
        val grimace = grimacePhaseIndex(calendarDay)
        return ronaldMoonlight(calendarDay) +
            grimaceMoonlight(calendarDay) +
            getHamburglarLight(ronald, grimace, miniMoonPosition(dayDifference))
    }

    /** Desktop [HolidayDatabase.getHamburglarLight]. */
    fun getHamburglarLight(ronaldPhase: Int, grimacePhase: Int, hamburglarPosition: Int): Int =
        when (hamburglarPosition) {
            0 -> if (grimacePhase > 0 && grimacePhase < 5) -1 else 1
            1 -> if (grimacePhase < 4) 1 else -1
            2 -> if (grimacePhase > 3) 1 else 0
            4 -> if (grimacePhase > 0 && grimacePhase < 5) 1 else 0
            5 -> if (ronaldPhase > 3) 1 else 0
            7 -> if (ronaldPhase > 0 && ronaldPhase < 5) 1 else 0
            8 -> if (ronaldPhase > 0 && ronaldPhase < 5) -1 else 1
            9 -> if (ronaldPhase < 4) 1 else -1
            10 -> {
                var total = 0
                if (ronaldPhase > 3) total++
                if (grimacePhase > 0 && grimacePhase < 5) total++
                total
            }
            else -> 0
        }

    fun getGameHoliday(calendarDay: Int = dayInKoLYear()): String? {
        val (month, day) = calendarComponents(calendarDay)
        return GAME_HOLIDAYS[month to day]
    }

    fun getGameHolidayInDays(daysAhead: Int, calendarDay: Int = dayInKoLYear()): String? =
        getGameHoliday((calendarDay + daysAhead) % 96)

    /** Desktop [HolidayDatabase.getEvents] — holidays + Labor Day Eve + stat day. */
    fun getEvents(dayDifference: Long = kolRolloverDayDifference()): List<String> {
        val list = getHolidays(dayDifference).toMutableList()
        val calendarDay = dayInKoLYear(dayDifference)
        if (getGameHolidayInDays(1, calendarDay) == "Lab&oacute;r Day") {
            list += "Lab&oacute;r Day Eve"
        }
        when (getStatDay(calendarDay)) {
            "muscle" -> list += "Muscle Day"
            "mysticality" -> list += "Mysticality Day"
            "moxie" -> list += "Moxie Day"
        }
        return list
    }

    /** Desktop [HolidayDatabase.getHolidaySummary] — today/tomorrow/in N days countdown. */
    fun getHolidaySummary(dayDifference: Long = kolRolloverDayDifference()): String {
        val today = getHoliday(dayDifference)
        if (today.isNotBlank()) return getDayCountAsString(0, today)
        val calendarDay = dayInKoLYear(dayDifference)
        for (i in 0 until 96) {
            val holiday = getGameHolidayInDays(i, calendarDay) ?: continue
            return getDayCountAsString(i, holiday)
        }
        return ""
    }

    fun getDayCountAsString(dayCount: Int, event: String): String {
        val count = getDayCountAsString(dayCount)
        return if (dayCount > 1) "$count until $event" else "$event $count"
    }

    fun getPhaseName(phase: Int): String = PHASE_NAMES.getOrNull(phase) ?: "unknown"

    fun getRonaldPhaseAsString(calendarDay: Int = dayInKoLYear()): String =
        getPhaseName(ronaldPhaseIndex(calendarDay))

    fun getGrimacePhaseAsString(calendarDay: Int = dayInKoLYear()): String =
        getPhaseName(grimacePhaseIndex(calendarDay))

    /** Desktop [HolidayDatabase.getHamburglarPosition] from KoL day-difference. */
    fun miniMoonPosition(dayDifference: Long = kolRolloverDayDifference()): Int {
        val daysSinceCollision = dayDifference - COLLISION_DAY_DIFFERENCE
        if (daysSinceCollision < 0) return -1
        return ((daysSinceCollision * 2 % 11 + 11) % 11).toInt()
    }

    fun getMiniMoonName(position: Int): String = MINI_MOON_NAMES.getOrNull(position) ?: "unknown"

    fun getMiniMoonAsString(dayDifference: Long = kolRolloverDayDifference()): String =
        getMiniMoonName(miniMoonPosition(dayDifference))

    fun getMoonEffect(calendarDay: Int = dayInKoLYear()): String =
        STAT_EFFECT.getOrNull(phaseStep(calendarDay)) ?: "Could not determine moon phase."

    /**
     * Desktop [HolidayDatabase.getStatDay] — which primary-stat moon bonus applies on [calendarDay].
     * Returns `"muscle"` / `"mysticality"` / `"moxie"`, or `""` when none.
     */
    fun getStatDay(calendarDay: Int = dayInKoLYear()): String = when (calendarDay % 16) {
        0, 15 -> "moxie"
        4, 12 -> "mysticality"
        8, 9 -> "muscle"
        else -> ""
    }

    fun getDayCountAsString(dayCount: Int): String = when (dayCount) {
        0 -> "today"
        1 -> "tomorrow"
        else -> "$dayCount days"
    }

    fun calendarDayOf(month: Int, day: Int): Int = (month - 1) * 8 + (day - 1)

    /** Desktop [HolidayDatabase.getHolidayPredictions] KoL-holiday offsets (no real-life overlay). */
    fun getHolidayPredictions(calendarDay: Int = dayInKoLYear()): List<String> {
        return GAME_HOLIDAYS.map { (md, name) ->
            val holidayDay = calendarDayOf(md.first, md.second)
            val offset = (holidayDay - calendarDay + 96) % 96
            Triple(offset, name, "$name: ${getDayCountAsString(offset)}")
        }.sortedWith(compareBy({ it.first }, { it.second }))
            .map { it.third }
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
