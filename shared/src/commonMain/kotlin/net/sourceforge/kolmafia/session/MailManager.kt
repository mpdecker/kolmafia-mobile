package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.preferences.Preferences

/** Small, UI-independent mailbox cache for the mobile client. */
data class MailMessage(
    val id: String,
    val sender: String,
    val senderId: String,
    val date: String,
    val html: String,
    val mailbox: String,
)

object MailManager {
    private val boxes = mutableMapOf<String, MutableList<MailMessage>>()
    private val messageStart = Regex("""<td\s+valign\s*=\s*['"]?top['"]?>(.*?)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val idPattern = Regex("""name\s*=\s*['"]?(\d+)""", RegexOption.IGNORE_CASE)
    private val senderPattern = Regex(
        """showplayer\.php\?who=(\d+).*?<b>(.*?)</b>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val datePattern = Regex("""Date\s*:?\s*</?[^>]*>\s*([^<]+)""", RegexOption.IGNORE_CASE)

    fun clear() = boxes.clear()

    fun messages(mailbox: String = "Inbox"): List<MailMessage> =
        boxes[mailbox].orEmpty().toList()

    fun hasNewMessages(preferences: Preferences): Boolean {
        val latest = messages("Inbox").firstOrNull()?.id ?: return false
        val old = preferences.getString("lastMessageId", "")
        preferences.setString("lastMessageId", latest)
        return latest != old
    }

    /**
     * Parses the message blocks used by messages.php. Unknown formatting is
     * retained in [MailMessage.html] rather than discarded.
     */
    fun parseMailbox(mailbox: String, html: String): List<MailMessage> {
        val starts = messageStart.findAll(html).map { it.range.first }.toList()
        if (starts.isEmpty()) return emptyList()
        val parsed = starts.mapIndexedNotNull { index, start ->
            val end = starts.getOrNull(index + 1) ?: html.length
            parseMessage(mailbox, html.substring(start, end))
        }
        val box = boxes.getOrPut(mailbox) { mutableListOf() }
        for (message in parsed) {
            if (box.none { it.id == message.id }) box += message
        }
        box.sortByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }
        return parsed
    }

    private fun parseMessage(mailbox: String, raw: String): MailMessage? {
        val id = idPattern.find(raw)?.groupValues?.get(1) ?: return null
        val senderMatch = senderPattern.find(raw)
        val senderId = senderMatch?.groupValues?.get(1).orEmpty()
        val sender = stripTags(senderMatch?.groupValues?.get(2).orEmpty()).ifBlank { "System" }
        if (senderId.isNotBlank()) PlayerIdRegistry.register(sender, senderId)
        val date = datePattern.find(raw)?.groupValues?.get(1)?.trim().orEmpty()
        return MailMessage(id, sender, senderId, date, raw, mailbox)
    }

    private fun stripTags(value: String): String =
        value.replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").trim()
}
