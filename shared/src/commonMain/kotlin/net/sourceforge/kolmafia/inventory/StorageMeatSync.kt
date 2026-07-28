package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase

/**
 * Parses storage meat and pulls remaining from storage.php?which=5 HTML.
 * Mirrors desktop [StorageRequest.parseStorage].
 */
object StorageMeatSync {

    private val STORAGE_MEAT = Regex("""<b>You have ([\d,]+) meat in long-term storage\.</b>""")
    private val FIST_STORAGE_MEAT = Regex("""thinking about the ([\d,]+) you currently have""")
    private val PULLS_LEFT = Regex("""<span class="pullsleft">(\d+)</span>""")

    fun parseStorageMeat(html: String, fistcore: Boolean): Long? {
        if (html.contains("Hagnk doesn't have any of your meat")) return 0L
        val pattern = if (fistcore) FIST_STORAGE_MEAT else STORAGE_MEAT
        return pattern.find(html)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
    }

    fun parsePullsRemaining(html: String, restricted: Boolean): Int =
        PULLS_LEFT.find(html)?.groupValues?.get(1)?.toIntOrNull()
            ?: if (restricted) 0 else -1

    fun apply(character: KoLCharacter, html: String, url: String?) {
        if (url == null || !url.contains("storage.php", ignoreCase = true)) return
        if (!url.contains("which=5")) return
        val state = character.state.value
        val fistcore = state.isFistcore
        parseStorageMeat(html, fistcore)?.let { character.setStorageMeat(it) }
        val restricted = state.isHardcore || state.isRestricted
        ConcoctionDatabase.setPullsRemaining(parsePullsRemaining(html, restricted))
    }
}
