package net.sourceforge.kolmafia.mall

import platform.Foundation.NSDate

internal actual fun currentEpochSeconds(): Long = NSDate().timeIntervalSince1970.toLong()
