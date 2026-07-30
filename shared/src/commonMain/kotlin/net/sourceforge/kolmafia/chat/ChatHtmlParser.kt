package net.sourceforge.kolmafia.chat

/** HTML parsers for internal chat command responses (`/who`, `/whois`). */
object ChatHtmlParser {

    private val tagPattern = Regex("<[^>]+>")
    private val parenthesisPattern = Regex(" \\(.*?\\)")

    internal val playerIdPattern =
        Regex("""showplayer\.php\?who=([-\d]+)['"][^>]*?>(.*?)</a>""", RegexOption.IGNORE_CASE)

    internal val whoPattern =
        Regex("""<font color='?#?(\w+)'?[^>]*>(.*?)</font></a>""", RegexOption.IGNORE_CASE)

    fun cleanPlayerName(raw: String): String {
        var name = tagPattern.replace(raw, "")
        name = parenthesisPattern.replace(name, "")
        return name.replace(":", "").trim()
    }

    fun parsePlayerIds(html: String) {
        playerIdPattern.findAll(html).forEach { match ->
            val playerId = match.groupValues[1]
            val playerName = cleanPlayerName(match.groupValues[2])
            if (playerName.isNotEmpty() && !playerName.startsWith("&")) {
                PlayerIdRegistry.register(playerName, playerId)
            }
        }
    }

    fun parseWhoClan(html: String): Map<String, Boolean> {
        val contacts = sortedMapOf<String, Boolean>()
        whoPattern.findAll(html).forEach { match ->
            val color = match.groupValues[1]
            val playerName = cleanPlayerName(match.groupValues[2])
            if (playerName.isEmpty()) return@forEach
            val inChat = color.equals("black", ignoreCase = true) ||
                color.equals("blue", ignoreCase = true)
            contacts[playerName] = inChat
        }
        return contacts
    }
}
