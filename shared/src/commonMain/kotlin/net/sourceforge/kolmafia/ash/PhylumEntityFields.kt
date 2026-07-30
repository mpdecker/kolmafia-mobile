package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.PhylumNames

/**
 * Resolves `$phylum[field]` bracket access. Mirrors desktop PhylumProxy metadata.
 */
internal object PhylumEntityFields {

    fun resolve(phylumRef: String, fieldName: String): AshValue {
        return when (fieldName.lowercase()) {
            "image" -> AshValue.of(PhylumNames.getImage(phylumRef))
            else -> throw ScriptException("phylum has no field '$fieldName'")
        }
    }
}
