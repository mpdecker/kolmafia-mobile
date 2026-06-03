package net.sourceforge.kolmafia.modifiers

data class ModifierValues(
    val doubles: Map<DoubleModifier, Double> = emptyMap(),
    val booleans: Map<BooleanModifier, Boolean> = emptyMap(),
    val strings: Map<StringModifier, List<String>> = emptyMap(),
    val bitmaps: Map<BitmapModifier, Int> = emptyMap()
) {
    // Numeric modifier value (0.0 if not present)
    fun get(mod: DoubleModifier): Double = doubles[mod] ?: 0.0
    fun getInt(mod: DoubleModifier): Int = get(mod).toInt()

    // Boolean flag (false if not present)
    fun get(mod: BooleanModifier): Boolean = booleans[mod] ?: false

    // String modifier — first value if multiple exist
    fun get(mod: StringModifier): String? = strings[mod]?.firstOrNull()
    fun getAll(mod: StringModifier): List<String> = strings[mod] ?: emptyList()

    // Bitmap modifier value (0 if not present)
    fun get(mod: BitmapModifier): Int = bitmaps[mod] ?: 0

    val isEmpty: Boolean
        get() = doubles.isEmpty() && booleans.isEmpty() && strings.isEmpty() && bitmaps.isEmpty()

    // Accumulates two ModifierValues together.
    // Doubles are summed, booleans are OR-ed, strings are merged into lists, bitmaps are OR-ed.
    operator fun plus(other: ModifierValues): ModifierValues {
        if (other.isEmpty) return this
        if (this.isEmpty) return other
        return ModifierValues(
            doubles = buildMap {
                putAll(doubles)
                other.doubles.forEach { (k, v) -> put(k, (doubles[k] ?: 0.0) + v) }
            },
            booleans = booleans + other.booleans,
            strings = buildMap {
                putAll(strings)
                other.strings.forEach { (k, vs) ->
                    put(k, (strings[k] ?: emptyList()) + vs)
                }
            },
            bitmaps = buildMap {
                putAll(bitmaps)
                other.bitmaps.forEach { (k, v) -> put(k, (bitmaps[k] ?: 0) or v) }
            }
        )
    }

    companion object {
        val EMPTY = ModifierValues()
    }
}
