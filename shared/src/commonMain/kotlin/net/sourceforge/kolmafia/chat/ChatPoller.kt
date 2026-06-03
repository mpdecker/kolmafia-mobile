package net.sourceforge.kolmafia.chat

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ChatPoller(private val httpClient: HttpClient) {

    var lastTime: String = "0"
        private set

    @Volatile
    private var listeners: List<(List<ChatMessage>) -> Unit> = emptyList()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    fun onMessages(listener: (List<ChatMessage>) -> Unit) {
        listeners = listeners + listener
    }

    suspend fun pollOnce() {
        fetchAndDispatch()
    }

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                val delayMs = fetchAndDispatch()
                delay(delayMs)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchAndDispatch(): Long {
        return try {
            val body = httpClient
                .get("$KOL_BASE_URL/newchatmessages.php") { parameter("lasttime", lastTime) }
                .bodyAsText()
            val response = ChatParser.parse(body)
            lastTime = response.lastTime
            val snapshot = listeners
            if (response.messages.isNotEmpty()) {
                snapshot.forEach { it(response.messages) }
            }
            response.delayMillis
        } catch (_: Exception) {
            RETRY_DELAY_MS
        }
    }

    companion object {
        private const val RETRY_DELAY_MS = 3_000L
    }
}
