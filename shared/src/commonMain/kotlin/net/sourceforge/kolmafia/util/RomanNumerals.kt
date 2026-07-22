package net.sourceforge.kolmafia.util

/** Desktop [StringUtilities.parseRomanNumerals]. */
object RomanNumerals {

    private val values = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000,
    )

    fun parse(roman: String): Int {
        var value = 0
        for (i in roman.length downTo 1) {
            val current = values[roman[i - 1]] ?: continue
            val next = if (i < roman.length) values[roman[i]] else null
            val sign = if (next != null && current < next) -1 else 1
            value += sign * current
        }
        return value
    }
}
