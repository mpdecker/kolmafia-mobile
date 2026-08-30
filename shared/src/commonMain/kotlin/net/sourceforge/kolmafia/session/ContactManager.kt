package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.chat.PlayerIdRegistry

/** Mail-contact parsing without the desktop Swing contact window. */
object ContactManager {
    private val contacts = linkedSetOf<String>()
    private val entryPattern = Regex(
        """<a\s+href=['"]showplayer\.php\?who=(\d+)['"][^>]*>.*?<b>(.*?)</b>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun reset() {
        contacts.clear()
    }

    fun isMailContact(name: String): Boolean =
        contacts.any { it.equals(name.trim(), ignoreCase = true) }

    fun mailContacts(): List<String> = contacts.toList()

    fun registerPlayerId(name: String, id: String) {
        PlayerIdRegistry.register(name, id)
    }

    fun updateFromHtml(html: String): List<String> {
        contacts.clear()
        for (match in entryPattern.findAll(html)) {
            val name = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
            if (name.isNotBlank()) {
                contacts += name
                registerPlayerId(name, match.groupValues[1])
            }
        }
        return mailContacts()
    }

    fun playerId(name: String): String = PlayerIdRegistry.getPlayerId(name)
    fun playerName(id: String): String = PlayerIdRegistry.getPlayerName(id)
}
