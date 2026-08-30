package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import kotlin.time.TimeSource

/** Minimal raw request used by the headless ping facility. */
class PingRequest(
    private val client: HttpClient,
    page: String = "api",
) {
    val page: String = normalizePage(page)
    var responseText: String? = null
        private set
    var redirectLocation: String? = null
        private set
    private var elapsed: Long = 0

    suspend fun run(): Result<String> {
        if (page !in VALID_PAGES) return Result.failure(IllegalArgumentException("Unknown ping page: $page"))
        redirectLocation = null
        val mark = TimeSource.Monotonic.markNow()
        return try {
            val response = client.get("$KOL_BASE_URL/${target(page)}") {
                when (page) {
                    "(status)" -> {
                        parameter("what", "status")
                        parameter("for", "KoLmafia")
                    }
                    "(events)" -> {
                        parameter("what", "events")
                        parameter("for", "KoLmafia")
                    }
                }
            }
            responseText = response.bodyAsText()
            redirectLocation = response.headers[HttpHeaders.Location]
                ?: response.request.url.toString()
                    .takeIf { it.substringBefore("?") != "$KOL_BASE_URL/${target(page)}" }
            elapsed = if (redirectLocation == null) mark.elapsedNow().inWholeMilliseconds else 0
            Result.success(responseText.orEmpty())
        } catch (e: Exception) {
            responseText = null
            elapsed = 0
            Result.failure(e)
        }
    }

    fun getElapsedTime(): Long = elapsed

    companion object {
        val VALID_PAGES = setOf("api", "council", "main", "(status)", "(events)")
        fun normalizePage(page: String?): String {
            val value = page?.trim()?.lowercase().orEmpty()
            val dot = value.indexOf(".php")
            return when (if (dot >= 0) value.substring(0, dot) else value) {
                "status" -> "(status)"
                "events" -> "(events)"
                else -> if (dot >= 0) value.substring(0, dot) else value
            }
        }
        private fun target(page: String): String = when (page) {
            "(status)", "(events)", "api" -> "api.php"
            "council" -> "council.php"
            "main" -> "main.php"
            else -> "(none)"
        }
    }
}
