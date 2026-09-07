package net.sourceforge.kolmafia.ash

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.http.KOL_BASE_URL

internal fun GameRuntimeLibrary.registerWebHtml(scope: AshScope) {
    val stringStringMap = AggregateType(AshType.STRING, AshType.STRING)

    fun loadHtml(url: String, encoded: Boolean): String {
        val lower = url.lowercase()
        if (lower.endsWith(".htm") || lower.endsWith(".html")) {
            return net.sourceforge.kolmafia.platform.UserDataFileIO.readText(url)
                ?: net.sourceforge.kolmafia.platform.UserDataFileIO.readText("html/$url")
                ?: ""
        }
        val client = httpClient ?: return ""
        val fullUrl = if (encoded || url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "$KOL_BASE_URL/${url.trimStart('/')}"
        }
        lastVisitPath = fullUrl
        return runBlocking {
            try {
                val body = client.get(fullUrl).body<String>()
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

    fun bufferOf(text: String) = AshValue(AshType.BUFFER, StringBuilder(text))

    regFn(scope, "load_html", AshType.BUFFER, listOf("url" to AshType.STRING)) { _, args ->
        bufferOf(loadHtml(args[0].toString(), encoded = false))
    }

    regFn(scope, "load_html", AshType.BUFFER,
        listOf("url" to AshType.STRING, "encoded" to AshType.BOOLEAN)) { _, args ->
        bufferOf(loadHtml(args[0].toString(), args[1].toBoolean()))
    }

    regFn(scope, "form_field", AshType.STRING,
        listOf("html" to AshType.STRING, "name" to AshType.STRING)) { _, args ->
        val fields = HtmlFormParser.parseFirstForm(args[0].toString())
        AshValue.of(fields[args[1].toString()] ?: "")
    }

    regFn(scope, "make_url", AshType.STRING,
        listOf("base" to AshType.STRING, "params" to stringStringMap)) { _, args ->
        val base = args[0].toString()
        val agg = args[1] as AggregateValue
        val query = agg.map.entries.joinToString("&") { (k, v) ->
            "${k.toString().encodeURLParameter()}=${v.toString().encodeURLParameter()}"
        }
        AshValue.of(if (query.isEmpty()) base else "$base?$query")
    }

    // Desktop make_url(location, usePostMethod, encoded)
    regFn(scope, "make_url", AshType.STRING,
        listOf(
            "location" to AshType.STRING,
            "usePostMethod" to AshType.BOOLEAN,
            "encoded" to AshType.BOOLEAN,
        )) { _, args ->
        val location = args[0].toString()
        val encoded = args[2].toBoolean()
        AshValue.of(
            if (encoded || location.startsWith("http://") || location.startsWith("https://")) {
                location
            } else {
                location.trimStart('/')
            },
        )
    }
}
