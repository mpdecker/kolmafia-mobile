package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop [net.sourceforge.kolmafia.request.ProfileRequest] — minimal hardcore/ronin parse. */
data class ProfileRequest(
    val playerName: String,
    val playerId: String,
    val isHardcore: Boolean = false,
    val inRonin: Boolean = false,
) {
    val canInteract: Boolean get() = !isHardcore && !inRonin

    companion object {
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

        fun parse(html: String, playerName: String, playerId: String): ProfileRequest =
            ProfileRequest(
                playerName = playerName,
                playerId = playerId,
                isHardcore = html.contains("<b>(Hardcore)</b></td>"),
                inRonin = html.contains("<b>(In Ronin)</b>"),
            )

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
