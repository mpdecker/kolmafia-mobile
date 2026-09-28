package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop `UseItemRequest.parseGiftPackage`: a gift package whose use page names the sender
 * session-logs `Opening <item> from <sender>`.
 */
object UseItemGiftPackageSync {
    private val FROM = Regex(
        """<p>From: <b><a class=nounder href="showplayer\.php\?who=(\d+)">(.*?)</a></b>""",
    )

    fun parse(
        responseText: String,
        itemId: Int,
        sessionLogger: SessionLogger? = null,
        itemName: (Int) -> String = { id ->
            ItemDatabase.getItemName(id).ifBlank { "item #$id" }
        },
        isGiftPackage: (Int) -> Boolean = { ItemDatabase.isGiftPackage(it) },
    ) {
        if (itemId <= 0 || !isGiftPackage(itemId)) return
        val from = FROM.find(responseText)?.groupValues?.getOrNull(2) ?: return
        if (from.isEmpty()) return
        RequestLogger.updateSessionLog(
            "Opening ${itemName(itemId)} from $from",
            sessionLogger,
        )
    }
}
