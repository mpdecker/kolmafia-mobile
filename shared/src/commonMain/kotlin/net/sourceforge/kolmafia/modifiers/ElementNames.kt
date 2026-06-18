package net.sourceforge.kolmafia.modifiers

/**
 * ASH elemental damage types. Aligns with [elementalResistanceModifier] in AshP23.
 */
object ElementNames {

    private val CANONICAL = listOf(
        "cold", "hot", "sleaze", "spooky", "stench", "slime", "supercold",
    )

    private val BY_LOWER = CANONICAL.associateBy { it.lowercase() }

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null
        return BY_LOWER[trimmed.lowercase()]
    }

    fun isValid(name: String): Boolean = resolve(name) != null
}
