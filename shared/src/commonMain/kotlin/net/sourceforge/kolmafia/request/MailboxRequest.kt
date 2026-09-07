package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.MailManager

/** Desktop [net.sourceforge.kolmafia.request.MailboxRequest] messages.php hub. */
object MailboxRequest {
    fun parseResponse(url: String, html: String) {
        if (!url.contains("messages.php", ignoreCase = true) &&
            !url.contains("mail.php", ignoreCase = true)
        ) {
            return
        }
        val mailbox = Regex("""(?:box|mailbox)=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1) ?: "Inbox"
        MailManager.parseMailbox(mailbox, html)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("messages.php", ignoreCase = true) ||
            url.contains("mail.php", ignoreCase = true)
}
