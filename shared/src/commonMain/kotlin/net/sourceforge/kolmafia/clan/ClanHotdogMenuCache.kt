package net.sourceforge.kolmafia.clan

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ClanManager.clanHotdogs v1 — session pref cache until per-clan id sync exists. */
object ClanHotdogMenuCache {

    const val CACHED_HOT_DOG_STAND_MENU_PREF = "_cachedHotDogStandMenu"

    fun saveMenu(prefs: Preferences?) {
        if (prefs == null) return
        val names = HotDogAvailability.snapshotNames()
        if (names.isEmpty()) return
        prefs.setString(CACHED_HOT_DOG_STAND_MENU_PREF, names.joinToString(","))
    }

    fun restoreIntoAvailability(prefs: Preferences?) {
        if (prefs == null) return
        if (!prefs.getBoolean(ClanLoungeSync.CLAN_HAS_HOT_DOG_STAND_PREF, false)) return
        if (!HotDogAvailability.isEmpty()) return
        val cached = prefs.getString(CACHED_HOT_DOG_STAND_MENU_PREF, "")
        if (cached.isBlank()) return
        for (segment in cached.split(",")) {
            val name = segment.trim()
            if (name.isNotEmpty()) {
                HotDogAvailability.restoreName(name)
            }
        }
        if (!HotDogAvailability.isEmpty()) {
            ConcoctionDatabase.refreshAfterLoungeMutation(prefs)
        }
    }
}
