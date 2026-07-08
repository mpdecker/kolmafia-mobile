package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.modifiers.PathNames
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Resolves `$path[field]` bracket access. Mirrors desktop [PathProxy].
 */
internal object PathEntityFields {

    fun resolve(pathName: String, fieldName: String, preferences: Preferences?): AshValue {
        val canonical = PathNames.resolve(pathName) ?: pathName
        val path = AscensionPath.fromApiString(canonical)
        val known = path != AscensionPath.UNKNOWN
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(if (known) path.pathId.toLong() else 0L)
            "name" -> AshValue.of(if (known) path.apiName else "")
            "avatar" -> AshValue.of(if (known) path.avatarPath else false)
            "image" -> AshValue.of(if (known) path.pathImage else "")
            "points" -> {
                val pref = if (known) path.pointsPreference else null
                val points = pref?.let { preferences?.getInt(it, 0) } ?: 0
                AshValue.of(points.toLong())
            }
            "familiars" -> AshValue.of(if (known) path.canUseFamiliars() else true)
            else -> throw ScriptException("path has no field '$fieldName'")
        }
    }
}
