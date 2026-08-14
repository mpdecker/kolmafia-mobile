package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [IslandManager.ensureUpdatedBigIsland] / [IslandManager.resetIsland] ascension reset. */
object IslandWarResetSync {

    const val PREF_LAST_BATTLEFIELD_RESET = "lastBattlefieldReset"

    /**
     * Reset island war prefs when [ascensionNumber] exceeds [PREF_LAST_BATTLEFIELD_RESET].
     * Returns true when a reset ran.
     */
    fun ensureUpdated(ascensionNumber: Int, preferences: Preferences): Boolean {
        val lastAscension = preferences.getInt(PREF_LAST_BATTLEFIELD_RESET, -1)
        if (lastAscension >= ascensionNumber) {
            return false
        }
        preferences.setInt(PREF_LAST_BATTLEFIELD_RESET, ascensionNumber)
        resetIsland(preferences)
        return true
    }

    /** Desktop [IslandManager.resetIsland] — clear battlefield/sidequest prefs for a new ascension. */
    fun resetIsland(preferences: Preferences) {
        preferences.setInt("fratboysDefeated", 0)
        preferences.setInt("hippiesDefeated", 0)
        preferences.setString("sidequestArenaCompleted", "none")
        preferences.setString("sidequestFarmCompleted", "none")
        preferences.setString("sidequestJunkyardCompleted", "none")
        preferences.setString("sidequestLighthouseCompleted", "none")
        preferences.setString("sidequestNunsCompleted", "none")
        preferences.setString(
            "sidequestOrchardCompleted",
            preferences.getString("currentHippyStore", "none"),
        )
        preferences.setString("currentJunkyardTool", "")
        preferences.setString("currentJunkyardLocation", "")
        preferences.setInt("currentNunneryMeat", 0)
        preferences.setInt("lastFratboyCall", -1)
        preferences.setInt("lastHippyCall", -1)
        preferences.setInt("availableDimes", 0)
        preferences.setInt("availableQuarters", 0)
        preferences.setString("sideDefeated", "neither")
        preferences.setString("warProgress", "unstarted")
        preferences.setInt("flyeredML", 0)
    }
}
