package net.sourceforge.kolmafia.chat

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ChatPoller(private val httpClient: HttpClient) {

    var lastTime: String = "0"
        private set

    private val listeners = mutableListOf<(List<ChatMessage>) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    fun onMessages(listener: (List<ChatMessage>) -> Unit) {
        listeners += listener
    }

    suspend fun pollOnce() {
        try {
            val body = httpClient
                .get("$KOL_BASE_URL/newchatmessages.php") { parameter("lasttime", lastTime) }
                .bodyAsText()
            val response = ChatParser.parse(body)
            lastTime = response.lastTime
            if (response.messages.isNotEmpty()) {
                listeners.forEach { it(response.messages) }
            }
        } catch (_: Exception) {
            // Network errors are non-fatal; next poll will retry
        }
    }

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                val body = runCatching {
                    httpClient.get("$KOL_BASE_URL/newchatmessages.php") {
                        parameter("lasttime", lastTime)
                    }.bodyAsText()
                }.getOrNull()
                if (body != null) {
                    val response = ChatParser.parse(body)
                    lastTime = response.lastTime
                    if (response.messages.isNotEmpty()) {
                        listeners.forEach { it(response.messages) }
                    }
                    delay(response.delayMillis)
                } else {
                    delay(3_000L)
                }
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
