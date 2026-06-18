package net.sourceforge.kolmafia.modifiers

/**
 * Resolves ASH modifier entity names. Mirrors desktop [ModifierDatabase.byCaselessName]
 * and [ModifierDatabase.numericByCaselessName] (enum tags, not modifiers.txt rows).
 */
object ModifierNames {

    private val byLowerName: Map<String, String> = buildMap {
        for (mod in DoubleModifier.entries) {
            put(mod.name.lowercase(), mod.tag)
            put(mod.tag.lowercase(), mod.tag)
        }
        for (mod in DerivedModifier.entries) {
            put(mod.name.lowercase(), mod.displayName)
            put(mod.displayName.lowercase(), mod.displayName)
        }
        for (mod in BitmapModifier.entries) {
            put(mod.name.lowercase(), mod.tag)
            put(mod.tag.lowercase(), mod.tag)
        }
        for (mod in StringModifier.entries) {
            put(mod.name.lowercase(), mod.tag)
            put(mod.tag.lowercase(), mod.tag)
        }
        for (mod in BooleanModifier.entries) {
            put(mod.name.lowercase(), mod.tag)
            put(mod.tag.lowercase(), mod.tag)
        }
    }

    fun byCaselessName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null
        return byLowerName[trimmed.lowercase()]
    }

    fun isValid(name: String): Boolean = byCaselessName(name) != null
}
