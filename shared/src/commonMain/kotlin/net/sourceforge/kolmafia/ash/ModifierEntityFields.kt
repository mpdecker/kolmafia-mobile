package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ModifierNames

/**
 * Resolves `$modifier[field]` bracket access. Mirrors desktop ModifierProxy metadata.
 */
internal object ModifierEntityFields {

    fun resolve(modifierRef: String, fieldName: String): AshValue {
        return when (fieldName.lowercase()) {
            "name" -> AshValue.of(ModifierNames.canonicalName(modifierRef))
            "type" -> AshValue.of(ModifierNames.valueType(modifierRef))
            else -> throw ScriptException("modifier has no field '$fieldName'")
        }
    }
}
