package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.data.ModifierDatabase

/**
 * ASH path name resolution via modifiers.txt Path entries and [AscensionPath] API names.
 */
object PathNames {

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        lookupPath(trimmed)?.name?.let { return it }

        val path = AscensionPath.fromApiString(trimmed)
        if (path != AscensionPath.UNKNOWN) {
            lookupPath(path.apiName)?.name?.let { return it }
            return path.apiName
        }

        return null
    }

    fun isValid(name: String): Boolean = resolve(name) != null

    private fun lookupPath(name: String): net.sourceforge.kolmafia.data.ModifierEntry? {
        ModifierDatabase.getPath(name)?.let { return it }
        val map = ModifierDatabase.byTypeAndName["Path"] ?: return null
        return map.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }
}
