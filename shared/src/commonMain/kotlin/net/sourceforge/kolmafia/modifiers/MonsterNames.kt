package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase

/**
 * ASH monster name resolution. Canonical name from bundled [monsters.txt].
 */
object MonsterNames {

    fun resolve(name: String, database: GameDatabase? = null): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        trimmed.toIntOrNull()?.let { id ->
            val fromId = database?.monster(id) ?: MonsterDatabase.getById(id)
            if (fromId != null) return fromId.name
        }

        val fromName = database?.monster(trimmed) ?: MonsterDatabase.getByName(trimmed)
        return fromName?.name
    }

    fun isValid(name: String, database: GameDatabase? = null): Boolean = resolve(name, database) != null
}
