package net.sourceforge.kolmafia.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

/** Fetch TCRS dump files from the upstream KoLmafia GitHub repository. */
object TCRSRemoteFetch {
    const val REMOTE_BASE =
        "https://raw.githubusercontent.com/kolmafia/kolmafia/main/data/TCRS/"

    private val fetchedThisSession = mutableSetOf<String>()

    fun resetSessionCacheForTest() {
        fetchedThisSession.clear()
    }

    suspend fun fetchText(client: HttpClient, filename: String): FetchResult {
        val url = REMOTE_BASE + filename
        if (url in fetchedThisSession) {
            return FetchResult.AlreadyFetched(filename)
        }
        val body = try {
            client.get(url).bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return FetchResult.Failed(filename, e.message ?: "request failed")
        }
        if (body.isBlank()) {
            return FetchResult.Empty(filename)
        }
        fetchedThisSession += url
        return FetchResult.Success(filename, body)
    }

    sealed interface FetchResult {
        val filename: String

        data class Success(override val filename: String, val text: String) : FetchResult
        data class AlreadyFetched(override val filename: String) : FetchResult
        data class Empty(override val filename: String) : FetchResult
        data class Failed(override val filename: String, val reason: String) : FetchResult
    }
}
