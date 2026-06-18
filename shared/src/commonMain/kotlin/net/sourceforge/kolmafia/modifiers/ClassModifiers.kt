package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.CharacterClass

/**
 * Static ascension-class modifier strings for ASH CLASS entity lookups.
 * Desktop stores these via [ModifierType.CLASS] at runtime; mobile uses this table for
 * standard classes (Stat Tuning matches prime-stat EXP distribution).
 */
object ClassModifiers {

    fun modifierString(className: String): String? {
        val cls = resolveClass(className) ?: return null
        if (!cls.isStandardClass) return null
        return """Stat Tuning: "${cls.primeStatName}""""
    }

    fun resolveClass(className: String): CharacterClass? {
        if (className.isBlank()) return null
        CharacterClass.entries.firstOrNull {
            it.displayName.equals(className, ignoreCase = true)
        }?.let { return it }
        return className.toIntOrNull()?.let { CharacterClass.fromId(it) }
            ?.takeIf { it != CharacterClass.UNKNOWN }
    }
}
