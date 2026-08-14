package net.sourceforge.kolmafia.ash

import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

private val KOL_ROLLOVER = ZoneOffset.of("-03:30")

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun currentDateString(): String = SimpleDateFormat("yyyyMMdd").format(Date())
actual fun currentDateTimeString(): String = SimpleDateFormat("yyyyMMdd HH:mm:ss").format(Date())
actual fun currentTimeOfDayString(): String = SimpleDateFormat("HH:mm:ss").format(Date())

actual fun kolRolloverDayDifference(): Long {
    val now = ZonedDateTime.now(KOL_ROLLOVER)
    val newYear = ZonedDateTime.of(2005, 9, 17, 0, 0, 0, 0, KOL_ROLLOVER)
    val boundary = ZonedDateTime.of(2005, 10, 27, 0, 0, 0, 0, KOL_ROLLOVER)
    var days = ChronoUnit.DAYS.between(newYear, now)
    if (now.isAfter(boundary)) {
        days -= 1
    }
    return days
}
