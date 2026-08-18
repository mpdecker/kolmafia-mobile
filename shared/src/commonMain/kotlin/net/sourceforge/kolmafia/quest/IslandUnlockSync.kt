package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager] main.php island unlock — lastIslandUnlock = ascensions
 * when the map HTML contains island.php.
 */
object IslandUnlockSync {

    fun applyFromMain(
        url: String?,
        html: String,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("main.php", ignoreCase = true)) return false
        if (preferences.getInt("lastIslandUnlock", -1) == ascensionNumber) return false
        if (!html.contains("island.php")) return false
        preferences.setInt("lastIslandUnlock", ascensionNumber)
        return true
    }
}
