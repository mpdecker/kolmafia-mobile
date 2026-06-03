package net.sourceforge.kolmafia.modifiers

object ModifierParser {

    fun parse(modifierString: String): ModifierValues {
        if (modifierString.isBlank() || modifierString == "none") return ModifierValues.EMPTY

        val doubles = mutableMapOf<DoubleModifier, Double>()
        val booleans = mutableMapOf<BooleanModifier, Boolean>()
        val strings = mutableMapOf<StringModifier, MutableList<String>>()
        val bitmaps = mutableMapOf<BitmapModifier, Int>()

        for (token in splitTokens(modifierString)) {
            val t = token.trim()
            if (t.isEmpty()) continue

            val colonIdx = findColonOutsideQuotes(t)
            if (colonIdx < 0) {
                // Boolean flag — no value
                BooleanModifier.byTag(t)?.let { booleans[it] = true }
                continue
            }

            val tagStr = t.substring(0, colonIdx).trim()
            val valStr = t.substring(colonIdx + 1).trim()

            // Try DoubleModifier first
            val dmod = DoubleModifier.byTag(tagStr)
            if (dmod != null) {
                val v = parseDouble(valStr)
                if (dmod.multiple) {
                    doubles[dmod] = (doubles[dmod] ?: 0.0) + v
                } else {
                    doubles[dmod] = (doubles[dmod] ?: 0.0) + v
                }
                continue
            }

            // Try StringModifier
            val smod = StringModifier.byTag(tagStr)
            if (smod != null) {
                val s = valStr.trim('"')
                strings.getOrPut(smod) { mutableListOf() }.add(s)
                continue
            }

            // Try BitmapModifier
            val bmod = BitmapModifier.byTag(tagStr)
            if (bmod != null) {
                val v = parseDouble(valStr).toInt()
                bitmaps[bmod] = (bitmaps[bmod] ?: 0) or v
                continue
            }

            // Try BooleanModifier with value (shouldn't normally happen, but handle it)
            BooleanModifier.byTag(tagStr)?.let { booleans[it] = valStr != "0" }
        }

        return ModifierValues(
            doubles = doubles,
            booleans = booleans,
            strings = strings.mapValues { it.value.toList() },
            bitmaps = bitmaps
        )
    }

    // Split on commas that are NOT inside double-quotes or square brackets.
    private fun splitTokens(s: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0          // bracket depth
        var inQuote = false
        var start = 0

        for (i in s.indices) {
            when (s[i]) {
                '"'  -> inQuote = !inQuote
                '['  -> if (!inQuote) depth++
                ']'  -> if (!inQuote) depth--
                ','  -> if (!inQuote && depth == 0) {
                    result.add(s.substring(start, i))
                    start = i + 1
                }
            }
        }
        result.add(s.substring(start))
        return result
    }

    // Find the first colon that is NOT inside double-quotes or square brackets.
    private fun findColonOutsideQuotes(s: String): Int {
        var inQuote = false
        var depth = 0
        for (i in s.indices) {
            when (s[i]) {
                '"' -> inQuote = !inQuote
                '[' -> if (!inQuote) depth++
                ']' -> if (!inQuote) depth--
                ':' -> if (!inQuote && depth == 0) return i
            }
        }
        return -1
    }

    // Parse a numeric value from a modifier string.
    // Values starting with '[' are expressions not yet evaluated → return 0.0.
    private fun parseDouble(s: String): Double {
        val trimmed = s.trim()
        if (trimmed.startsWith('[')) return 0.0              // expression
        return trimmed.trimStart('+').toDoubleOrNull() ?: 0.0
    }
}
