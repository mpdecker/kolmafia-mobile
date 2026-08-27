package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClanMembersRequest(private val client: HttpClient) {
    open suspend fun fetchMembers(detailed: Boolean = false): Result<String> =
        fetch(if (detailed) "clan_detailedroster.php" else "showclan.php").also { result ->
            result.onSuccess { parseResponse(if (detailed) "clan_detailedroster.php" else "showclan.php", it) }
        }

    open suspend fun fetchRanks(): Result<String> =
        fetch("clan_members.php").also { result ->
            result.onSuccess { parseResponse("clan_members.php", it) }
        }

    private suspend fun fetch(path: String): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/$path")
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val MEMBER_PATTERN = Regex(
            """<a\s+class=nounder\s+href=["']showplayer\.php\?who=(\d+)["']>([^<]+)</a></b>&nbsp;</td><td\s+class=small>([^<]*?)&nbsp;</td><td\s+class=small>(\d+).*?</td>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val SIMPLE_MEMBER_PATTERN = Regex(
            """showplayer\.php\?who=(\d+)[^>]*>([^<]+)</a>""",
            RegexOption.IGNORE_CASE,
        )
        private val RANK_PATTERN = Regex(
            """<select\s+name=level.*?</select>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val OPTION_PATTERN = Regex("""<option[^>]*>(.*?)</option>""", RegexOption.IGNORE_CASE)

        fun parseResponse(url: String, html: String) {
            when {
                url.contains("clan_members.php", ignoreCase = true) -> {
                    val ranks = RANK_PATTERN.find(html)?.value?.let { select ->
                        OPTION_PATTERN.findAll(select).map {
                            it.groupValues[1].replace(Regex("<.*?>"), "").trim().lowercase()
                        }.filter { it.isNotEmpty() }.toList()
                    }.orEmpty()
                    if (ranks.isNotEmpty()) ClanManager.setRanks(ranks)
                }
                url.contains("showclan.php", ignoreCase = true) ||
                    url.contains("clan_detailedroster.php", ignoreCase = true) -> {
                    var found = false
                    MEMBER_PATTERN.findAll(html).forEach {
                        found = true
                        ClanManager.registerMember(
                            name = it.groupValues[2].trim(),
                            playerId = it.groupValues[1],
                            title = it.groupValues[3].replace("&nbsp;", "").trim(),
                        )
                    }
                    if (!found) {
                        SIMPLE_MEMBER_PATTERN.findAll(html).forEach {
                            ClanManager.registerMember(it.groupValues[2].trim(), it.groupValues[1])
                        }
                    }
                }
            }
        }
    }
}
