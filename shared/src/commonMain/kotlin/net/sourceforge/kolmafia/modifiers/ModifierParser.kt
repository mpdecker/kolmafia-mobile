package net.sourceforge.kolmafia.modifiers

object ModifierParser {

    /**
     * Parse a modifier string with full expression evaluation.
     * Use this when you have a live character state snapshot.
     */
    fun parse(modifierString: String, context: ExpressionContext): ModifierValues {
        if (modifierString.isBlank() || modifierString == "none") return ModifierValues.EMPTY
        return doParse(modifierString, context)
    }

    /**
     * Parse a modifier string without expression evaluation (expressions → 0.0).
     * Use this for static display (item browser, etc.) when no character state is available.
     */
    fun parse(modifierString: String): ModifierValues =
        parse(modifierString, ExpressionContext.EMPTY)

    // ── Implementation ────────────────────────────────────────────────────────

    private fun doParse(s: String, ctx: ExpressionContext): ModifierValues {
        val doubles  = mutableMapOf<DoubleModifier, Double>()
        val booleans = mutableMapOf<BooleanModifier, Boolean>()
        val strings  = mutableMapOf<StringModifier, MutableList<String>>()
        val bitmaps  = mutableMapOf<BitmapModifier, Int>()

        for (token in splitTokens(s)) {
            val t = token.trim()
            if (t.isEmpty()) continue

            val colonIdx = findColonOutsideQuotes(t)
            if (colonIdx < 0) {
                BooleanModifier.byTag(t)?.let { booleans[it] = true }
                continue
            }

            val tagStr = t.substring(0, colonIdx).trim()
            val valStr = t.substring(colonIdx + 1).trim()

            val dmod = DoubleModifier.byTag(tagStr)
            if (dmod != null) {
                val v = parseNumericValue(valStr, ctx)
                doubles[dmod] = (doubles[dmod] ?: 0.0) + v
                continue
            }

            val smod = StringModifier.byTag(tagStr)
            if (smod != null) {
                strings.getOrPut(smod) { mutableListOf() }.add(valStr.trim('"'))
                continue
            }

            val bmod = BitmapModifier.byTag(tagStr)
            if (bmod != null) {
                val v = parseNumericValue(valStr, ctx).toInt()
                bitmaps[bmod] = (bitmaps[bmod] ?: 0) or v
                continue
            }

            BooleanModifier.byTag(tagStr)?.let { booleans[it] = valStr != "0" }
        }

        return ModifierValues(
            doubles  = doubles,
            booleans = booleans,
            strings  = strings.mapValues { it.value.toList() },
            bitmaps  = bitmaps
        )
    }

    // ── Numeric value / expression parsing ───────────────────────────────────

    internal fun parseNumericValue(s: String, ctx: ExpressionContext): Double {
        val trimmed = s.trim()
        return when {
            trimmed.startsWith('[') -> ModifierExpression.evaluate(trimmed, ctx)
            else -> trimmed.trimStart('+').toDoubleOrNull() ?: 0.0
        }
    }

    // ── Token splitting — respects quoted strings and [bracket] expressions ───

    private fun splitTokens(s: String): List<String> {
        val result = mutableListOf<String>()
        var depth   = 0
        var inQuote = false
        var start   = 0
        for (i in s.indices) {
            when (s[i]) {
                '"'  -> inQuote = !inQuote
                '['  -> if (!inQuote) depth++
                ']'  -> if (!inQuote) depth--
                ','  -> if (!inQuote && depth == 0) { result.add(s.substring(start, i)); start = i + 1 }
            }
        }
        result.add(s.substring(start))
        return result
    }

    private fun findColonOutsideQuotes(s: String): Int {
        var inQuote = false
        var depth   = 0
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
}
