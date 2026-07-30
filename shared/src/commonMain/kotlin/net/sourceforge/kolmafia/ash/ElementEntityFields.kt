package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ElementNames

/**
 * Resolves `$element[field]` bracket access. Mirrors desktop ElementProxy metadata.
 */
internal object ElementEntityFields {

    fun resolve(elementRef: String, fieldName: String): AshValue {
        return when (fieldName.lowercase()) {
            "image" -> AshValue.of(ElementNames.getImage(elementRef))
            else -> throw ScriptException("element has no field '$fieldName'")
        }
    }
}
