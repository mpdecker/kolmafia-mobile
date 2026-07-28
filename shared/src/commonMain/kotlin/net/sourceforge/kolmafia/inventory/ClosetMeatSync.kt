package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.KoLCharacter

/**
 * Parses closet meat from closet.php HTML. Mirrors desktop [ClosetRequest.parseCloset].
 */
object ClosetMeatSync {

    private val CLOSET_MEAT = Regex("""Your closet contains <b>([\d,]+)</b> meat\.""")

    fun parseClosetMeat(html: String): Long? =
        CLOSET_MEAT.find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()

    fun apply(character: KoLCharacter, html: String, url: String?) {
        if (url != null && !url.contains("closet.php", ignoreCase = true)) return
        parseClosetMeat(html)?.let { character.setClosetMeat(it) }
    }
}
