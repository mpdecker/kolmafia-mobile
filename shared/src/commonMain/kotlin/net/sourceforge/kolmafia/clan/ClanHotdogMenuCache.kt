package net.sourceforge.kolmafia.clan

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ClanManager.clanHotdogs — per-clan hotdog menu cache with legacy pref fallback. */
object ClanHotdogMenuCache {

    const val CACHED_HOT_DOG_STAND_MENU_PREF = "_cachedHotDogStandMenu"

    private fun prefKey(clanId: Int): String =
        if (clanId != 0) "${CACHED_HOT_DOG_STAND_MENU_PREF}_$clanId" else CACHED_HOT_DOG_STAND_MENU_PREF

    fun saveMenu(prefs: Preferences?) {
        if (prefs == null) return
        val names = HotDogAvailability.snapshotNames()
        if (names.isEmpty()) return
        val clanId = ClanManager.getClanId()
        names.forEach { ClanManager.addHotdog(it) }
        prefs.setString(prefKey(clanId), names.joinToString(","))
    }

    fun restoreIntoAvailability(prefs: Preferences?) {
        if (prefs == null) return
        if (!prefs.getBoolean(ClanLoungeSync.CLAN_HAS_HOT_DOG_STAND_PREF, false)) return
        if (!HotDogAvailability.isEmpty()) return

        val clanId = ClanManager.getClanId()
        val fromManager = ClanManager.getHotdogs()
        if (fromManager.isNotEmpty()) {
            fromManager.forEach { HotDogAvailability.restoreName(it) }
        } else {
            val cached = prefs.getString(prefKey(clanId), "")
                .ifBlank { prefs.getString(CACHED_HOT_DOG_STAND_MENU_PREF, "") }
            if (cached.isBlank()) return
            for (segment in cached.split(",")) {
                val name = segment.trim()
                if (name.isNotEmpty()) {
                    HotDogAvailability.restoreName(name)
                }
            }
        }

        if (!HotDogAvailability.isEmpty()) {
            ConcoctionDatabase.refreshAfterLoungeMutation(prefs)
        }
    }
}
