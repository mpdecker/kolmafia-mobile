package net.sourceforge.kolmafia.ash

expect fun currentTimeMillis(): Long
expect fun currentDateString(): String      // "YYYYMMDD" in local time
expect fun currentDateTimeString(): String  // "YYYYMMDD HH:mm:ss" in local time
