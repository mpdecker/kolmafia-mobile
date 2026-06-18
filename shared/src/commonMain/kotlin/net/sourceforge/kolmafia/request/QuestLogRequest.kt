package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.preferences.Preferences

open class QuestLogRequest(
    private val client: HttpClient,
    private val questDatabase: QuestDatabase,
    private val preferences: Preferences? = null,
) {
    open suspend fun syncAll() {
        syncPage(1)
        syncPage(2)
        syncPage(3)
    }

    internal suspend fun syncPage(which: Int) {
        try {
            val response = client.get("$KOL_BASE_URL/questlog.php") {
                parameter("which", which.toString())
            }
            if (!response.status.isSuccess()) return
            parsePage(response.bodyAsText(), which)
        } catch (_: Exception) {
            // Network errors are non-fatal; quest state stays as-is
        }
    }

    internal fun parsePage(html: String, which: Int = 1) {
        if (which == 1) {
            clearAbsentCompletedQuestPrefs(html)
        }
        val sections = html.split(Regex("<b>", RegexOption.IGNORE_CASE))
        for (section in sections.drop(1)) {
            val closeIdx = section.indexOf("</b>", ignoreCase = true)
            if (closeIdx < 0) continue
            val title    = section.substring(0, closeIdx).trim()
            val bodyHtml = section.substring(closeIdx + 4)
            val entry = QuestLogDatabase.findByTitle(title) ?: continue
            val step  = QuestLogDatabase.detectStep(entry, bodyHtml)
            questDatabase.setProgressByPrefKey(entry.prefKey, step)
        }
    }

    private fun clearAbsentCompletedQuestPrefs(html: String) {
        val prefs = preferences ?: return
        if (!html.contains("Don't be Afraid of Any Ghost")) {
            prefs.setString("ghostLocation", "")
        }
        if (!html.contains("New-You VIP Club")) {
            prefs.setString("_newYouQuestMonster", "")
            prefs.setString("_newYouQuestSkill", "")
            prefs.setInt("_newYouQuestSharpensDone", 0)
            prefs.setInt("_newYouQuestSharpensToDo", 0)
        }
        if (!html.contains("Doctor, Doctor")) {
            prefs.setString("doctorBagQuestItem", "")
            prefs.setString("doctorBagQuestLocation", "")
        }
        if (!html.contains("Toot!")) {
            questDatabase.setProgress(Quest.TOOT, QuestDatabase.FINISHED)
        }
    }
}
