package net.sourceforge.kolmafia.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ChatProbe(private val httpClient: HttpClient) {

    open suspend fun sendInternalCommand(graf: String): Result<String> = try {
        val response = httpClient.submitForm(
            url = "$KOL_BASE_URL/submitnewchat.php",
            formParameters = parameters {
                append("graf", graf)
                append("j", "1")
            },
        )
        val body = response.bodyAsText()
        ChatHtmlParser.parsePlayerIds(body)
        Result.success(body)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun isPlayerOnline(player: String): Boolean {
        val name = player.trim()
        if (name.isEmpty()) return false
        val body = sendInternalCommand("/whois $name").getOrNull() ?: return false
        return body.contains(PLAYER_ONLINE_MARKER)
    }

    open suspend fun slashCount(itemId: Int): Int {
        val itemName = ItemDatabase.getItemName(itemId)
        if (itemName.isBlank()) return 0
        val body = sendInternalCommand("/count $itemName").getOrNull() ?: return 0
        return COUNT_PATTERN.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    open suspend fun whoClan(): Map<String, Boolean> {
        val body = sendInternalCommand("/who clan").getOrNull() ?: return emptyMap()
        return ChatHtmlParser.parseWhoClan(body)
    }

    open suspend fun lookupPlayerId(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return name
        sendInternalCommand("/whois $trimmed")
        return PlayerIdRegistry.getPlayerId(trimmed, retrieveId = false)
    }

    open suspend fun lookupPlayerName(id: String): String {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return id
        sendInternalCommand("/whois $trimmed")
        return PlayerIdRegistry.getPlayerName(trimmed, retrieveName = false)
    }

    companion object {
        internal const val PLAYER_ONLINE_MARKER = "This player is currently"
        internal val COUNT_PATTERN = Regex("""You have (\d+) """)
    }
}
