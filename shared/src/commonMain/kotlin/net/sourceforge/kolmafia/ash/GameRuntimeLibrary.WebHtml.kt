package net.sourceforge.kolmafia.ash

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.http.KOL_BASE_URL

internal fun GameRuntimeLibrary.registerWebHtml(scope: AshScope) {
    val stringStringMap = AggregateType(AshType.STRING, AshType.STRING)

    fun loadHtml(url: String, encoded: Boolean): String {
        val client = httpClient ?: return ""
        val fullUrl = if (encoded || url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "$KOL_BASE_URL/${url.trimStart('/')}"
        }
        return runBlocking {
            try {
                val body = client.get(fullUrl).body<String>()
                if (body.isNotBlank()) processVisitQuestHooks(body, url = fullUrl)
                body
            } catch (_: Exception) {
                ""
            }
        }
    }

    regFn(scope, "load_html", AshType.STRING, listOf("url" to AshType.STRING)) { _, args ->
        AshValue.of(loadHtml(args[0].toString(), encoded = false))
    }

    regFn(scope, "load_html", AshType.STRING,
        listOf("url" to AshType.STRING, "encoded" to AshType.BOOLEAN)) { _, args ->
        AshValue.of(loadHtml(args[0].toString(), args[1].toBoolean()))
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
}
