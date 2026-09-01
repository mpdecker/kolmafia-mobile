package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.StringModifier

/**
 * Derive TCRS rows from desc_item HTML — desktop [TCRSDatabase.deriveItem] parity.
 */
object TCRSDeriver {

    fun deriveFromHtml(itemId: Int, html: String): TCRSDatabase.TcrsEntry {
        val name = DescriptionParser.parseName(html)
        val size = DescriptionParser.parseConsumableSize(html)
        val quality = DescriptionParser.parseQuality(html)
        val parsed = ItemEnchantmentParser.parseItemEnchantments(html)
        val carried = ModifierDatabase.carriedOverModifiersForItem(itemId)
        val modifiers = mergeModifiers(parsed, carried)
        return TCRSDatabase.TcrsEntry(name, size, quality, modifiers)
    }

    fun deriveFromCache(itemId: Int): TCRSDatabase.TcrsEntry? {
        val cached = DescriptionCache.itemDescription(itemId)
        if (cached.isBlank()) return null
        return deriveFromHtml(itemId, cached)
    }

    private fun mergeModifiers(parsed: String, carried: String): String {
        if (carried.isBlank()) return parsed
        if (parsed.isBlank()) return carried
        val tokens = parsed.split(',').map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        for (token in carried.split(',').map { it.trim() }.filter { it.isNotBlank() }) {
            if (tokens.none { existing -> existing.startsWith(token.substringBefore(':')) }) {
                tokens += token
            }
        }
        return tokens.joinToString(", ")
    }
}
