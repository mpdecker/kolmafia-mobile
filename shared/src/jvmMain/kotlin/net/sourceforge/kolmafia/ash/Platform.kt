package net.sourceforge.kolmafia.ash

import java.text.SimpleDateFormat
import java.util.Date

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
actual fun currentDateString(): String = SimpleDateFormat("yyyyMMdd").format(Date())
actual fun currentDateTimeString(): String = SimpleDateFormat("yyyyMMdd HH:mm:ss").format(Date())
