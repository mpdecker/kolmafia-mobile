package net.sourceforge.kolmafia.modifiers

data class ModifierValues(
    val doubles: Map<DoubleModifier, Double> = emptyMap(),
    val booleans: Map<BooleanModifier, Boolean> = emptyMap(),
    val strings: Map<StringModifier, List<String>> = emptyMap(),
    val bitmaps: Map<BitmapModifier, Int> = emptyMap()
) {
    // Numeric modifier value (0.0 if not present). Combined tags with a stored
    // zero derive the min of subsumed members, or 0.0 when members straddle zero.
    fun get(mod: DoubleModifier): Double {
        val value = doubles[mod] ?: 0.0
        if (mod.subsumed.isNotEmpty() && value == 0.0) {
            var min = Double.MAX_VALUE
            var max = -Double.MAX_VALUE
            for (sub in mod.subsumed) {
                val subValue = get(sub)
                min = minOf(min, subValue)
                max = maxOf(max, subValue)
            }
            return if (min < 0.0 && max > 0.0) 0.0 else min
        }
        return value
    }
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
    // Desktop Modifiers.add(): overlapping MUTEX bits accumulate into MUTEX_VIOLATIONS.
    operator fun plus(other: ModifierValues): ModifierValues {
        if (other.isEmpty) return this
        if (this.isEmpty) return other
        val leftMutex = get(BitmapModifier.MUTEX)
        val rightMutex = other.get(BitmapModifier.MUTEX)
        val newViolations = leftMutex and rightMutex
        val mergedViolations =
            get(BitmapModifier.MUTEX_VIOLATIONS) or
                other.get(BitmapModifier.MUTEX_VIOLATIONS) or
                newViolations
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
                BitmapModifier.entries.forEach { mod ->
                    if (mod == BitmapModifier.MUTEX_VIOLATIONS) return@forEach
                    val merged = this@ModifierValues.get(mod) or other.get(mod)
                    if (merged != 0) put(mod, merged)
                }
                if (mergedViolations != 0) {
                    put(BitmapModifier.MUTEX_VIOLATIONS, mergedViolations)
                }
            },
        )
    }

    companion object {
        val EMPTY = ModifierValues()
    }
}
