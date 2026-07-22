package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.ModifierEnchantmentParser

/**
 * Parses item description HTML into a modifiers.txt string.
 * Ported from desktop [DebugDatabase.parseItemEnchantments].
 */
object ItemEnchantmentParser {

    internal val ITEM_ENCHANTMENT = Regex(
        "<font color=\"?blue\"?>(?!\\(awesome\\)|<p>)(.*)(?:<br>)?</font>(?:<br />)?",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )

    fun parseItemEnchantments(html: String): String {
        var text = html
        val eindex = text.indexOf("Effect:")
        if (eindex != -1) {
            val spanStart = text.indexOf("<span", eindex)
            val spanEnd = text.indexOf("</span>", eindex)
            if (spanStart != -1 && spanEnd != -1) {
                text = text.removeRange(spanStart, spanEnd + 7)
            }
        }

        val known = StandardEnchantmentParser.ModifierAccumulator()
        StandardEnchantmentParser.parseStandardEnchantments(
            text,
            known,
            ITEM_ENCHANTMENT,
            damageReductionSource = text,
        )

        if (!known.contains("Damage Reduction")) {
            known.append(ModifierEnchantmentParser.parseDamageReduction(text))
        }

        known.append(ModifierEnchantmentParser.parseSkill(text))
        known.append(ModifierEnchantmentParser.parseSingleEquip(text))
        known.append(ModifierEnchantmentParser.parseSoftcoreOnly(text))
        known.append(ModifierEnchantmentParser.parseLastsOneDay(text))
        known.append(ModifierEnchantmentParser.parseFreePull(text))
        known.append(ModifierEnchantmentParser.parseNoPull(text))
        known.append(ModifierEnchantmentParser.parseEffect(text))
        known.append(ModifierEnchantmentParser.parseEffectDuration(text))
        known.append(ModifierEnchantmentParser.parseSongDuration(text))
        known.append(ModifierEnchantmentParser.parseDropsItems(text))
        known.append(ModifierEnchantmentParser.parseLastAvailable(text))

        return known.toString()
    }
}
