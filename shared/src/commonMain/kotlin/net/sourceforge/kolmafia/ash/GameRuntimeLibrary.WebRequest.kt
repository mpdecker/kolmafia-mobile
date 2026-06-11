package net.sourceforge.kolmafia.ash

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
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
                val body = response.body<String>()
                if (body.isNotBlank()) processVisitQuestHooks(body)
                body
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun doPost(url: String, postData: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded) url else "$KOL_BASE_URL/${url.trimStart('/')}"
        return runBlocking {
            try {
                val response = client.submitForm(
                    url = fullUrl,
                    formParameters = Parameters.build {
                        postData.split("&").filter { it.isNotBlank() }.forEach { pair ->
                            val eq = pair.indexOf('=')
                            if (eq >= 0) {
                                append(
                                    pair.substring(0, eq).decodeURLQueryComponent(),
                                    pair.substring(eq + 1).decodeURLQueryComponent()
                                )
                            } else append(pair.decodeURLQueryComponent(), "")
                        }
                    },
                )
                val body = response.body<String>()
                if (body.isNotBlank()) processVisitQuestHooks(body)
                body
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

    // visit_url(url, post_data) → string — POST with URL-encoded body
    regFn(scope, "visit_url", AshType.STRING,
        listOf("url" to AshType.STRING, "post_data" to AshType.STRING)) { _, args ->
        AshValue.of(doPost(args[0].toString(), args[1].toString(), encoded = false))
    }

    // visit_url(url, post_data, encoded) → string — POST, encoded flag controls base URL prepend
    regFn(scope, "visit_url", AshType.STRING,
        listOf("url" to AshType.STRING, "post_data" to AshType.STRING, "encoded" to AshType.BOOLEAN)) { _, args ->
        AshValue.of(doPost(args[0].toString(), args[1].toString(), args[2].toBoolean()))
    }
}
