package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop [net.sourceforge.kolmafia.request.ProfileRequest] — hardcore/ronin/fame/clan parse. */
data class ProfileRequest(
    val playerName: String,
    val playerId: String,
    val isHardcore: Boolean = false,
    val inRonin: Boolean = false,
    val pvpRank: Int = 0,
    val clanId: Int = -1,
    val clanName: String = "",
) {
    val canInteract: Boolean get() = !isHardcore && !inRonin

    companion object {
        private val clanPattern = Regex(
            """Clan:\s*<b><a[^>]*href="showclan\.php\?whichclan=(\d+)">(.*?)</a>""",
            RegexOption.IGNORE_CASE,
        )
        private val whoPattern = Regex("""(?:^|[?&])who=(\d+)""", RegexOption.IGNORE_CASE)

        fun fromPlayerName(raw: String): ProfileRequest {
            val trimmed = raw.trim()
            return if (trimmed.startsWith("#")) {
                val id = trimmed.substring(1)
                ProfileRequest(playerName = PlayerIdRegistry.getPlayerName(id), playerId = id)
            } else {
                ProfileRequest(
                    playerName = trimmed,
                    playerId = PlayerIdRegistry.getPlayerId(trimmed),
                )
            }
        }

        fun parse(html: String, playerName: String, playerId: String): ProfileRequest {
            val clanMatch = clanPattern.find(html)
            return ProfileRequest(
                playerName = playerName,
                playerId = playerId,
                isHardcore = html.contains("<b>(Hardcore)</b></td>"),
                inRonin = html.contains("<b>(In Ronin)</b>"),
                pvpRank = parseFame(html),
                clanId = clanMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1,
                clanName = clanMatch?.groupValues?.get(2).orEmpty(),
            )
        }

        fun applyFromVisit(html: String, url: String, character: KoLCharacter?) {
            val who = whoPattern.find(url)?.groupValues?.get(1) ?: return
            val name = PlayerIdRegistry.getPlayerName(who).ifEmpty { who }
            val profile = parse(html, name, who)
            if (character?.state?.value?.playerId?.toString() == who) {
                ClanManager.setClan(
                    id = profile.clanId.coerceAtLeast(0),
                    name = profile.clanName.takeIf { it.isNotEmpty() },
                )
            }
        }

        private fun parseFame(html: String): Int {
            val clean = html.replace("><", "").replace(Regex("<[^>]*>"), "\n")
            val lines = clean.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            val idx = lines.indexOfFirst { it.startsWith("Fame") }
            if (idx < 0 || idx + 1 >= lines.size) return 0
            return lines[idx + 1].replace(",", "").toIntOrNull() ?: 0
        }

        suspend fun retrieve(
            client: HttpClient,
            playerName: String,
            playerId: String,
        ): Result<ProfileRequest> = try {
            val response = client.get("$KOL_BASE_URL/showplayer.php?who=$playerId")
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                Result.success(parse(response.bodyAsText(), playerName, playerId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
