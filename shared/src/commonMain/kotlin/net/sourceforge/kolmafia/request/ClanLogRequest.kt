package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClanLogRequest(private val client: HttpClient) {
    open suspend fun fetch(): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/clan_log.php")
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else response.bodyAsText().let { html ->
            parseResponse("clan_log.php", html)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val TIME_REGEX = """(\d\d/\d\d/\d\d, \d\d:\d\d[AP]M)"""
        private const val PLAYER_REGEX =
            """<a class=nounder href=['"]showplayer\.php\?who=\d+['"]>([^<]*?) \(#\d+\)</a>"""
        private val STASH_PATTERN = Regex(
            """$TIME_REGEX:\s*$PLAYER_REGEX\s+(added|took)\s+([\d,]+)\s+(.*?)\.<br>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val WAR_PATTERN = Regex(
            """$TIME_REGEX:\s*([^<]*?) launched an attack against (.*?)\.<br>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

        fun parseResponse(url: String, html: String) {
            if (!url.contains("clan_log.php", ignoreCase = true)) return
            val lines = mutableListOf<String>()
            STASH_PATTERN.findAll(html).forEach {
                val direction = if (it.groupValues[3].equals("added", true)) "added to stash"
                    else "taken from stash"
                lines += "${it.groupValues[1]}: ${it.groupValues[2].trim()}: " +
                    "${it.groupValues[4]} ${it.groupValues[5].trim()} $direction"
            }
            WAR_PATTERN.findAll(html).forEach {
                lines += "${it.groupValues[1]}: ${it.groupValues[2].trim()}: " +
                    "${it.groupValues[3].trim()} attacked"
            }
            ClanManager.setStashLog(lines)
            ClanManager.saveStashLog()
        }
    }
}
