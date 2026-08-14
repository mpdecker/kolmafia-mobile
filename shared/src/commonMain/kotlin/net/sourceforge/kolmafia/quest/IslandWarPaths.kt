package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [IslandManager.currentIsland] path selection from war progress. */
object IslandWarPaths {

    fun currentIsland(preferences: Preferences): String =
        when (preferences.getString("warProgress", "unstarted")) {
            "finished" -> "postwarisland.php"
            "started" -> "bigisland.php"
            else -> "bogus.php"
        }

    /** Desktop [IslandManager.questCompleter] — hippy/fratboy pref → plural completer name. */
    fun questCompleter(preference: String, preferences: Preferences): String =
        when (preferences.getString(preference, "none")) {
            "hippy" -> "hippies"
            "fratboy" -> "fratboys"
            else -> "none"
        }
}
