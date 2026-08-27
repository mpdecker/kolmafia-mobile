package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

open class ClanWarRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
) {
    data class EnemyClan(val id: Int, val name: String) : Comparable<EnemyClan> {
        override fun compareTo(other: EnemyClan): Int = name.compareTo(other.name, ignoreCase = true)
        override fun toString(): String = name
    }

    open suspend fun fetchTargets(): Result<String> = fetch("clan_attack.php")
    open suspend fun fetchWait(): Result<String> = fetch("clan_war.php")

    private suspend fun fetch(path: String): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/$path")
        if (!response.status.isSuccess()) Result.failure(Exception("HTTP ${response.status.value}"))
        else response.bodyAsText().let { html ->
            parseResponse(path, html, preferences)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private val CLANID_PATTERN =
            Regex("""name=whichclan value=(\d+)></td><td><b>([^<]+)</td><td>([\d]+)</td>""")
        private val WAIT_PATTERN =
            Regex("""<br>Your clan can attack again in (.*?)<p>""", RegexOption.DOT_MATCHES_ALL)

        val enemyClans: MutableList<EnemyClan> = mutableListOf()
        var nextAttack: String = "You may attack right now."
            private set

        fun parseResponse(url: String, html: String, preferences: Preferences? = null) {
            when {
                url.contains("clan_attack.php", ignoreCase = true) -> {
                    enemyClans.clear()
                    CLANID_PATTERN.findAll(html).forEach {
                        if (it.groupValues[3].toIntOrNull() == 1) {
                            enemyClans += EnemyClan(
                                it.groupValues[1].toInt(),
                                it.groupValues[2].trim(),
                            )
                        }
                    }
                    enemyClans.sort()
                    if (enemyClans.isNotEmpty()) {
                        nextAttack = "You may attack right now."
                        preferences?.setBoolean("clanAttacksEnabled", true)
                    } else {
                        parseWaitTime(html, preferences)
                    }
                }
                url.contains("clan_war.php", ignoreCase = true) ->
                    parseWaitTime(html, preferences)
            }
        }

        private fun parseWaitTime(html: String, preferences: Preferences?) {
            val wait = WAIT_PATTERN.find(html)?.groupValues?.get(1)?.trim()
            if (wait != null) {
                nextAttack = "You may attack again in $wait"
                preferences?.setBoolean("clanAttacksEnabled", true)
            } else {
                nextAttack = "You do not have the ability to attack."
                preferences?.setBoolean("clanAttacksEnabled", false)
            }
        }

        internal fun resetForTest() {
            enemyClans.clear()
            nextAttack = "You may attack right now."
        }
    }
}
