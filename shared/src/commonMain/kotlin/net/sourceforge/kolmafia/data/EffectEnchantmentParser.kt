package net.sourceforge.kolmafia.data

/**
 * Parses effect description HTML into a modifiers.txt string.
 * Ported from desktop [DebugDatabase.parseEffectEnchantments].
 */
object EffectEnchantmentParser {

    private val EFFECT_ENCHANTMENT = Regex(
        "<font color=blue><b>(.*)</b></font>",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )

    fun parseEffectEnchantments(html: String): String {
        val known = StandardEnchantmentParser.ModifierAccumulator()
        StandardEnchantmentParser.parseStandardEnchantments(html, known, EFFECT_ENCHANTMENT)
        return known.toString()
    }
}
