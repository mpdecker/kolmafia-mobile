package net.sourceforge.kolmafia.modifiers

/** Serializes [ModifierValues] to a modifiers.txt-compatible comma-separated string. */
object ModifierValuesFormatter {

    fun format(values: ModifierValues): String {
        if (values.isEmpty) return ""
        val tokens = mutableListOf<String>()
        for ((mod, value) in values.doubles.entries.sortedBy { it.key.tag }) {
            if (value == 0.0) continue
            tokens += "${mod.tag}: ${formatNumeric(value)}"
        }
        for ((mod, value) in values.booleans.entries.sortedBy { it.key.tag }) {
            if (!value) continue
            tokens += mod.tag
        }
        for ((mod, value) in values.bitmaps.entries.sortedBy { it.key.tag }) {
            if (value == 0) continue
            if (mod == BitmapModifier.MUTEX || mod == BitmapModifier.MUTEX_VIOLATIONS) continue
            tokens += if (value == 1) mod.tag else "${mod.tag}: $value"
        }
        for ((mod, valueList) in values.strings.entries.sortedBy { it.key.tag }) {
            for (value in valueList) {
                if (value.isBlank()) continue
                tokens += "${mod.tag}: \"${value.replace("\"", "\\\"")}\""
            }
        }
        return tokens.joinToString(", ")
    }

    private fun formatNumeric(value: Double): String {
        if (value == value.toLong().toDouble()) {
            return if (value >= 0.0) "+${value.toLong()}" else value.toLong().toString()
        }
        val text = value.toString()
        return if (value > 0.0 && !text.startsWith('+')) "+$text" else text
    }
}
