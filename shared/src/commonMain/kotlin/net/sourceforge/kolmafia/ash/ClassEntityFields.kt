package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.modifiers.ClassModifiers
import net.sourceforge.kolmafia.modifiers.ClassNames
import net.sourceforge.kolmafia.modifiers.StatNames

/**
 * Resolves `$class[field]` bracket access. Mirrors desktop ClassProxy metadata.
 */
internal object ClassEntityFields {

    fun resolve(classRef: String, fieldName: String): AshValue {
        val cls = resolveClass(classRef)
        val known = cls != CharacterClass.UNKNOWN

        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(if (known) cls.id.toLong() else 0L)
            "primestat" -> AshValue(
                AshType.STAT,
                if (known) StatNames.resolve(cls.primeStatName) ?: cls.primeStatName else "",
            )
            "path" -> AshValue(
                AshType.PATH,
                if (known && cls.ascensionPath != AscensionPath.UNKNOWN) cls.ascensionPath.apiName else "",
            )
            else -> throw ScriptException("class has no field '$fieldName'")
        }
    }

    private fun resolveClass(classRef: String): CharacterClass {
        ClassModifiers.resolveClass(classRef)?.let { return it }
        ClassNames.resolve(classRef)?.let { resolved ->
            ClassModifiers.resolveClass(resolved)?.let { return it }
        }
        return CharacterClass.UNKNOWN
    }
}
