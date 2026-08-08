package net.sourceforge.kolmafia.clan

/** Parses clan id/name from charpane HTML (desktop ProfileRequest clan link). */
object ClanIdSync {

    private val CLAN_LINK =
        Regex("""showclan\.php\?whichclan=(\d+)[^>]*>([^<]+)</a>""", RegexOption.IGNORE_CASE)

    fun apply(html: String) {
        val match = CLAN_LINK.find(html)
        if (match != null) {
            val id = match.groupValues[1].toIntOrNull() ?: return
            val name = match.groupValues[2].trim()
            ClanManager.setClan(id, name)
            return
        }
        if (html.contains("You aren't in a clan", ignoreCase = true) ||
            html.contains("not in a clan", ignoreCase = true)
        ) {
            ClanManager.clearCache(newCharacter = false)
        }
    }
}
