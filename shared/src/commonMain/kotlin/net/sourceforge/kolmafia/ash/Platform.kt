package net.sourceforge.kolmafia.ash

expect fun currentTimeMillis(): Long
expect fun currentDateString(): String      // "YYYYMMDD" in local time
expect fun currentDateTimeString(): String  // "YYYYMMDD HH:mm:ss" in local time
expect fun currentTimeOfDayString(): String // "HH:mm:ss" in local time
expect fun kolRolloverDayDifference(): Long
expect fun kolTimeInKoLDayMillis(): Int
expect fun formatAshDateTime(format: String, millis: Long, timeZone: String?): String
expect fun parseAshDateTimestamp(inFormat: String, dateString: String): Long
expect fun formatAshTimestamp(millis: Long, outFormat: String): String
