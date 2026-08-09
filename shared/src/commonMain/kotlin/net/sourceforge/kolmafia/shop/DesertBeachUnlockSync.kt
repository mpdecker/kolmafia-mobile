package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.KoLCharacter.setDesertBeachAvailable]. */
object DesertBeachUnlockSync {
    fun setAvailable(ascensionNumber: Int, preferences: Preferences?) {
        val prefs = preferences ?: return
        if (prefs.getInt("lastDesertUnlock", -1) == ascensionNumber) return
        prefs.setInt("lastDesertUnlock", ascensionNumber)
    }
}
