package net.sourceforge.kolmafia.ash

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun currentTimeMillis(): Long = (NSDate.date().timeIntervalSince1970 * 1000).toLong()

actual fun currentDateString(): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyyMMdd"
    return fmt.stringFromDate(NSDate.date())
}

actual fun currentDateTimeString(): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyyMMdd HH:mm:ss"
    return fmt.stringFromDate(NSDate.date())
}

actual fun currentTimeOfDayString(): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "HH:mm:ss"
    return fmt.stringFromDate(NSDate.date())
}

actual fun kolRolloverDayDifference(): Long {
    // KoL rollover uses GMT-0330; approximate via UTC offset for catalog rotation index.
    val epochSeconds = NSDate.date().timeIntervalSince1970
    val rolloverSeconds = epochSeconds - (3.5 * 3600)
    val newYearSeconds = 1126915200.0 - (3.5 * 3600) // 2005-09-17 00:00 GMT-0330
    val boundarySeconds = 1130371200.0 - (3.5 * 3600) // 2005-10-27 00:00 GMT-0330
    var days = ((rolloverSeconds - newYearSeconds) / 86400.0).toLong()
    if (rolloverSeconds > boundarySeconds) {
        days -= 1
    }
    return days
}

actual fun kolTimeInKoLDayMillis(): Int {
    val epochSeconds = NSDate.date().timeIntervalSince1970
    val rolloverSeconds = epochSeconds - (3.5 * 3600)
    val midnightSeconds = rolloverSeconds - (rolloverSeconds.toLong() % 86400)
    return ((rolloverSeconds - midnightSeconds) * 1000.0).toInt().coerceAtLeast(0)
}

actual fun formatAshDateTime(format: String, millis: Long, timeZone: String?): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = format
  if (!timeZone.isNullOrBlank()) {
        fmt.timeZone = platform.Foundation.NSTimeZone.timeZoneWithName(timeZone)
    }
    return fmt.stringFromDate(platform.Foundation.NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}

actual fun parseAshDateTimestamp(inFormat: String, dateString: String): Long {
    val fmt = NSDateFormatter()
    fmt.dateFormat = inFormat
    val date = fmt.dateFromString(dateString) ?: return 0L
    return (date.timeIntervalSince1970 * 1000.0).toLong()
}

actual fun formatAshTimestamp(millis: Long, outFormat: String): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = outFormat
    return fmt.stringFromDate(platform.Foundation.NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
}
