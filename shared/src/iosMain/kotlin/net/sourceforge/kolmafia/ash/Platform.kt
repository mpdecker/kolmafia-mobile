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
