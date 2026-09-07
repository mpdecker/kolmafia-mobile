package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestSpecialSync
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Desktop [net.sourceforge.kolmafia.request.GuzzlrRequest] inventory tap hub. */
object GuzzlrRequest {
    fun parseResponse(
        url: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ) {
        if (!url.contains("inventory.php", ignoreCase = true) ||
            !url.contains("guzzlr", ignoreCase = true)
        ) {
            if (!html.contains("Guzzlr", ignoreCase = true)) return
        }
        QuestSpecialSync.parseGuzzlrSection(html, preferences, gameDatabase)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("tap=guzzlr", ignoreCase = true)
}
