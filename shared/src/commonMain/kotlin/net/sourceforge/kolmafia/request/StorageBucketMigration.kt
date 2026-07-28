package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [QuestManager.handleTimeTower] cached storage/freepull bucket moves. */
object StorageBucketMigration {

    fun migrateToolbelt(timeTowerAvailable: Boolean, prefs: Preferences) {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val storage = loadCache(prefs, Preferences.CACHED_STORAGE).toMutableMap()
        val freepulls = loadCache(prefs, Preferences.CACHED_FREEPULLS).toMutableMap()

        val source = if (timeTowerAvailable) storage else freepulls
        val dest = if (timeTowerAvailable) freepulls else storage

        val qty = source.remove(toolbeltId) ?: return
        if (qty <= 0) return

        dest[toolbeltId] = (dest[toolbeltId] ?: 0) + qty

        saveCache(prefs, Preferences.CACHED_STORAGE, storage)
        saveCache(prefs, Preferences.CACHED_FREEPULLS, freepulls)
    }

    private fun loadCache(prefs: Preferences, key: String): Map<Int, Int> {
        val raw = prefs.getString(key, "")
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull { part ->
            val pieces = part.split(':')
            if (pieces.size != 2) return@mapNotNull null
            val id = pieces[0].toIntOrNull() ?: return@mapNotNull null
            val qty = pieces[1].toIntOrNull() ?: return@mapNotNull null
            id to qty
        }.toMap()
    }

    private fun saveCache(prefs: Preferences, key: String, contents: Map<Int, Int>) {
        val encoded = contents.entries
            .filter { it.value > 0 }
            .joinToString("|") { "${it.key}:${it.value}" }
        prefs.setString(key, encoded)
    }
}
