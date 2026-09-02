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

actual fun kolTimeInKoLDayMillis(): Int {
    val now = ZonedDateTime.now(KOL_ROLLOVER)
    val midnight = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
    return ChronoUnit.MILLIS.between(midnight, now).toInt()
}

actual fun formatAshDateTime(format: String, millis: Long, timeZone: String?): String =
    runCatching {
        val fmt = SimpleDateFormat(format)
        if (!timeZone.isNullOrBlank()) {
            fmt.timeZone = java.util.TimeZone.getTimeZone(timeZone)
        }
        fmt.format(Date(millis))
    }.getOrElse { millis.toString() }

actual fun parseAshDateTimestamp(inFormat: String, dateString: String): Long =
    runCatching {
        SimpleDateFormat(inFormat).parse(dateString)?.time ?: 0L
    }.getOrDefault(0L)

actual fun formatAshTimestamp(millis: Long, outFormat: String): String =
    runCatching { SimpleDateFormat(outFormat).format(Date(millis)) }.getOrElse { millis.toString() }
