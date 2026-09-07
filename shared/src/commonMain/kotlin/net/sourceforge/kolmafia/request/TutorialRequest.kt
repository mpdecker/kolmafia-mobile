package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Desktop [net.sourceforge.kolmafia.request.TutorialRequest] Toot Oriole. */
object TutorialRequest {
    fun parseResponse(url: String, html: String, questDatabase: QuestDatabase?) {
        if (!url.contains("tutorial.php", ignoreCase = true)) return
        if (html.contains("You acquire an item:") ||
            html.contains("You've learned everything I can teach you")
        ) {
            questDatabase?.setProgress(Quest.TOOT, QuestDatabase.FINISHED)
        }
    }

    fun registerRequest(url: String): Boolean = url.contains("tutorial.php", ignoreCase = true)
}
