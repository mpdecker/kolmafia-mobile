package net.sourceforge.kolmafia.ash

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

internal fun GameRuntimeLibrary.registerWebRequests(scope: AshScope) {

    fun bufferOf(text: String) = AshValue(AshType.BUFFER, StringBuilder(text))

    fun doVisit(url: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded) url
                      else "$KOL_BASE_URL/${url.trimStart('/')}"
        lastVisitPath = fullUrl
        return runBlocking {
            try {
                val response = client.get(fullUrl)
                val body = response.body<String>()
                if (body.isNotBlank()) {
                    processVisitResponseHooks(body, url = fullUrl)
                    processVisitQuestHooks(body, url = fullUrl)
                }
                body
            } catch (_: Exception) {
                ""
            }
        }
    }

    fun doPost(url: String, postData: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded) url else "$KOL_BASE_URL/${url.trimStart('/')}"
        lastVisitPath = fullUrl
        ChoiceCombatAshState.setFormFieldsFromPostData(postData)
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
                if (body.isNotBlank()) {
                    processVisitResponseHooks(body, url = fullUrl)
                    processVisitQuestHooks(body, url = fullUrl)
                }
                body
            } catch (_: Exception) {
                ""
            }
        }
    }

    fun visit(location: String, usePostMethod: Boolean, encoded: Boolean): String {
        if (location.isBlank()) return ""
        return if (usePostMethod) doPost(location, "", encoded) else doVisit(location, encoded)
    }

    // Desktop: visit_url() — relay override buffer; headless empty.
    regFn(scope, "visit_url", AshType.BUFFER, emptyList()) { _, _ ->
        bufferOf("")
    }

    // Desktop: visit_url(url) posts by default.
    regFn(scope, "visit_url", AshType.BUFFER,
        listOf("url" to AshType.STRING)) { _, args ->
        bufferOf(visit(args[0].toString(), usePostMethod = true, encoded = false))
    }

    // Desktop: visit_url(url, usePostMethod)
    regFn(scope, "visit_url", AshType.BUFFER,
        listOf("url" to AshType.STRING, "usePostMethod" to AshType.BOOLEAN)) { _, args ->
        bufferOf(visit(args[0].toString(), args[1].toBoolean(), encoded = false))
    }

    // Desktop: visit_url(url, usePostMethod, encoded)
    regFn(scope, "visit_url", AshType.BUFFER,
        listOf(
            "url" to AshType.STRING,
            "usePostMethod" to AshType.BOOLEAN,
            "encoded" to AshType.BOOLEAN,
        )) { _, args ->
        bufferOf(visit(args[0].toString(), args[1].toBoolean(), args[2].toBoolean()))
    }

    // Extra: visit_url(url, post_data) — POST with URL-encoded body
    regFn(scope, "visit_url", AshType.BUFFER,
        listOf("url" to AshType.STRING, "post_data" to AshType.STRING)) { _, args ->
        bufferOf(doPost(args[0].toString(), args[1].toString(), encoded = false))
    }

    // Extra: visit_url(url, post_data, encoded)
    regFn(scope, "visit_url", AshType.BUFFER,
        listOf(
            "url" to AshType.STRING,
            "post_data" to AshType.STRING,
            "encoded" to AshType.BOOLEAN,
        )) { _, args ->
        bufferOf(doPost(args[0].toString(), args[1].toString(), args[2].toBoolean()))
    }
}
