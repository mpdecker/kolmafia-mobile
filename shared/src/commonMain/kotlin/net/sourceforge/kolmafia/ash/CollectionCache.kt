package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

/** Persists collection snapshots as `id:qty|id:qty` preference strings. */
internal object CollectionCache {
    fun save(preferences: Preferences, key: String, contents: Map<Int, Int>) {
        val encoded = contents.entries
            .filter { it.value > 0 }
            .joinToString("|") { "${it.key}:${it.value}" }
        preferences.setString(key, encoded)
    }

    fun load(preferences: Preferences, key: String): Map<Int, Int> {
        val raw = preferences.getString(key, "")
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull { part ->
            val pieces = part.split(':')
            if (pieces.size != 2) return@mapNotNull null
            val id = pieces[0].toIntOrNull() ?: return@mapNotNull null
            val qty = pieces[1].toIntOrNull() ?: return@mapNotNull null
            id to qty
        }.toMap()
    }
}
