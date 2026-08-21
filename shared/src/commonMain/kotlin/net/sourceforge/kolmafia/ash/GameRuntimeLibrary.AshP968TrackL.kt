package net.sourceforge.kolmafia.ash

/**
 * AshP968–969 Track L — Familiar lock sync registration anchor.
 * Live lock HTTP lives in AshP903 (first-match); visit sync below.
 */
internal fun GameRuntimeLibrary.registerAshP968TrackLBatch(scope: AshScope) {
    // AshP903 owns lock_familiar_equipment.
}

object FamiliarEquipmentLockSync {
    private val LOCK_REGEX = Regex("""familiar\.php\?action=lockequip""")
    private val LOCKED_REGEX = Regex("""Locked""")

    fun parseAndWrite(html: String, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        if ("familiar.php" !in html) return
        val hasLockLink = LOCK_REGEX.containsMatchIn(html)
        val isLocked = LOCKED_REGEX.containsMatchIn(html) && !hasLockLink
        prefs.setBoolean("familiarEquipmentLocked", isLocked)
    }
}
