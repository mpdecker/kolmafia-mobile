package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.ModifierEnchantmentParser
import net.sourceforge.kolmafia.modifiers.StringModifier

/** Shared desktop [DebugDatabase.parseStandardEnchantments] loop for item/effect HTML. */
internal object StandardEnchantmentParser {

    internal class ModifierAccumulator {
        private val entries = mutableListOf<ModifierEntry>()

        fun contains(name: String): Boolean = entries.any { it.name == name }

        fun append(mod: String?) {
            if (mod == null) return
            if (mod.contains('"') || !mod.contains(',')) {
                addSingle(mod)
                return
            }
            mod.split(',').forEach { addSingle(it) }
        }

        private fun addSingle(mod: String) {
            val trimmed = mod.trim()
            if (trimmed.isEmpty()) return
            val colon = trimmed.indexOf(':')
            val name = if (colon == -1) trimmed else trimmed.substring(0, colon).trim()
            val value = if (colon == -1) null else trimmed.substring(colon + 1).trim()
            addTo(name, value)
        }

        fun addTo(name: String, value: String?) {
            val existing = entries.indexOfFirst { it.name == name }
            if (existing < 0) {
                entries.add(ModifierEntry(name, value))
                return
            }
            val cur = entries[existing]
            if (value != null && cur.value != null && isNumeric(cur.value) && isNumeric(value)) {
                val sum = (cur.value.trimStart('+').toIntOrNull() ?: 0) +
                    (value.trimStart('+').toIntOrNull() ?: 0)
                entries[existing] = ModifierEntry(name, sum.toString())
            }
        }

        fun wrapValues(transform: (String) -> String) {
            for (i in entries.indices) {
                val v = entries[i].value ?: continue
                entries[i] = entries[i].copy(value = transform(v))
            }
        }

        override fun toString(): String = entries.joinToString(", ") { it.format() }

        private data class ModifierEntry(val name: String, val value: String? = null) {
            fun format(): String = if (value == null) name else "$name: $value"
        }
    }

    internal fun parseStandardEnchantments(
        text: String,
        known: ModifierAccumulator,
        pattern: Regex,
        damageReductionSource: String? = null,
    ) {
        val match = pattern.find(text) ?: return
        var enchantments = match.groupValues[1]
            .replace(
                "<b>NOTE:</b> Items that reduce the MP cost of skills will not do so by more than 3 points, in total.",
                "",
            )
            .replace("<br>", "\n")
            .replace("<Br>", "\n")
            .replace("<br />", "\n")
            .replace("</font></b></center>", "\n")
            .replace("<font color=\"blue\"></font>", "")

        val blueStart = "<font color=\"blue\">"
        val blueEnd = "</font>"
        val daggerFootnote = " <sup>&dagger;</sup>"
        var decemberEvent = false

        for (raw in enchantments.split("\n+".toRegex())) {
            var enchantment = raw.trim()
            if (enchantment.isEmpty()) continue

            if (enchantment.endsWith(daggerFootnote)) {
                enchantment = enchantment.removeSuffix(daggerFootnote)
            }
            if (enchantment == "(awesome)") continue

            if (enchantment.startsWith(blueStart)) {
                if (!enchantment.contains(blueEnd) || enchantment.endsWith(blueEnd)) {
                    enchantment = enchantment.removePrefix(blueStart)
                }
            }
            if (enchantment.endsWith(blueEnd)) {
                if (!enchantment.contains("<font")) {
                    enchantment = enchantment.removeSuffix(blueEnd)
                }
            }

            var mod = ModifierEnchantmentParser.parseModifier(enchantment)
            if (mod != null) {
                if (mod.startsWith("Rollover Effect Duration")) {
                    ModifierEnchantmentParser.parseStringModifier(enchantment)?.let { known.append(it) }
                } else if (mod.startsWith("Damage Reduction")) {
                    mod = ModifierEnchantmentParser.parseDamageReduction(
                        damageReductionSource ?: text,
                    )
                } else if (mod == "${StringModifier.CLASS.tag}: \"December\"") {
                    decemberEvent = true
                    continue
                }
                known.append(mod)
            }
        }

        if (decemberEvent) {
            known.wrapValues { value -> "[$value*event(December)]" }
        }
    }

    private fun isNumeric(s: String): Boolean = s.trimStart('+', '-').all { it.isDigit() }
}
