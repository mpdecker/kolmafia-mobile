package net.sourceforge.kolmafia.concoction

import net.sourceforge.kolmafia.character.KoLCharacter

/**
 * Parses cocktail still count from shop.php?whichshop=still HTML.
 * Mirrors desktop [StillRequest.parseResponse].
 */
object StillSync {

    private val STILLS_PATTERN = Regex("""with (\d+) bright""")

    fun parseStillsAvailable(html: String): Int? =
        STILLS_PATTERN.find(html)?.groupValues?.get(1)?.toIntOrNull()

    fun apply(character: KoLCharacter, html: String, url: String?) {
        if (url == null || !url.contains("shop.php", ignoreCase = true)) return
        if (!url.contains("whichshop=still", ignoreCase = true)) return
        parseStillsAvailable(html)?.let { character.setStillsAvailable(it) }
    }
}
