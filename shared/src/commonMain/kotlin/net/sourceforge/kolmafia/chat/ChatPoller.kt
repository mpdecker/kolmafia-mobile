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
    private var listener: ((List<ChatMessage>) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    fun setListener(l: (List<ChatMessage>) -> Unit) {
        listener = l
    }

    fun clearListener() {
        listener = null
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
            if (response.messages.isNotEmpty()) {
                listener?.invoke(response.messages)
            }
            response.delayMillis
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            RETRY_DELAY_MS
        }
    }

    companion object {
        private const val RETRY_DELAY_MS = 3_000L
    }
}
