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
