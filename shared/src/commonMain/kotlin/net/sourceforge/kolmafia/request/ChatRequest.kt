package net.sourceforge.kolmafia.request

/** Desktop [net.sourceforge.kolmafia.request.ChatRequest] newchatmessages.php URL builder. */
object ChatRequest {
    fun pollUrl(lastSeen: Long, tabbedChat: Boolean, afk: Boolean): String = buildString {
        append("newchatmessages.php?")
        if (tabbedChat) {
            append("j=1&")
        }
        append("lasttime=").append(lastSeen)
        if (!tabbedChat) {
            append("&afk=").append(if (afk) "1" else "0")
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("newchatmessages.php", ignoreCase = true) ||
            url.contains("submitnewchat.php", ignoreCase = true)
}
