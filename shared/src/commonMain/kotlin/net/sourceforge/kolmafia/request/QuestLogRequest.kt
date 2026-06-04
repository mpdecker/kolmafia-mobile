package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.quest.QuestDatabase

open class QuestLogRequest(
    private val client: HttpClient,
    private val questDatabase: QuestDatabase,
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
            parsePage(response.bodyAsText())
        } catch (_: Exception) {
            // Network errors are non-fatal; quest state stays as-is
        }
    }

    internal fun parsePage(html: String) {
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
}
