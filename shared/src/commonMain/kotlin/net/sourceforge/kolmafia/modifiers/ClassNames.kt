package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.CharacterClass

/**
 * ASH class name catalog. Resolves to canonical [CharacterClass.displayName].
 */
object ClassNames {

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        trimmed.toIntOrNull()?.let { id ->
            val fromId = CharacterClass.fromId(id)
            if (fromId != CharacterClass.UNKNOWN) return fromId.displayName
        }

        CharacterClass.entries.firstOrNull {
            it != CharacterClass.UNKNOWN &&
                it.displayName.equals(trimmed, ignoreCase = true)
        }?.let { return it.displayName }

        return null
    }

    fun isValid(name: String): Boolean = resolve(name) != null
}
