package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.data.ModifierDatabase

/**
 * ASH thrall name resolution via modifiers.txt Thrall entries.
 */
object ThrallNames {

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null
        return ModifierDatabase.getThrall(trimmed)?.name
    }

    fun isValid(name: String): Boolean = resolve(name) != null
}
