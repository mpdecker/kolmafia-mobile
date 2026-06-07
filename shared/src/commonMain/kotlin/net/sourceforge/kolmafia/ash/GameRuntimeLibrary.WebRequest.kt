package net.sourceforge.kolmafia.ash

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.http.KOL_BASE_URL

internal fun GameRuntimeLibrary.registerWebRequests(scope: AshScope) {

    fun doVisit(url: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded) url
                      else "$KOL_BASE_URL/${url.trimStart('/')}"
        return runBlocking {
            try {
                val response = client.get(fullUrl)
                response.body<String>()
            } catch (e: Exception) {
                ""
            }
        }
    }

    regFn(scope, "visit_url", AshType.STRING,
        listOf("url" to AshType.STRING)) { _, args ->
        AshValue.of(doVisit(args[0].toString(), encoded = false))
    }

    regFn(scope, "visit_url", AshType.STRING,
        listOf("url" to AshType.STRING, "encoded" to AshType.BOOLEAN)) { _, args ->
        AshValue.of(doVisit(args[0].toString(), args[1].toBoolean()))
    }
}
