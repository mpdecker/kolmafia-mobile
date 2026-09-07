package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CouncilVisitSync
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Desktop [net.sourceforge.kolmafia.request.CouncilRequest]. */
object CouncilRequest {
    fun path(kingdomOfExploathing: Boolean): String =
        if (kingdomOfExploathing) {
            "place.php?whichplace=exploathing&action=expl_council"
        } else {
            "council.php"
        }

    fun parseResponse(
        url: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        level: Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ) {
        if (!CouncilVisitSync.isCouncilUrl(url)) return
        CouncilVisitSync.applyFromVisit(url, html, questDatabase, preferences, level, consumeItem)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("council.php", ignoreCase = true) ||
            url.contains("action=expl_council", ignoreCase = true)
}
