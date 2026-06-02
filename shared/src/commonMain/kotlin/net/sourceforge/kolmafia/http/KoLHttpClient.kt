package net.sourceforge.kolmafia.http

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

const val KOL_BASE_URL = "https://www.kingdomofloathing.com"

fun createKoLHttpClient(): HttpClient = HttpClient {
    install(HttpCookies)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(HttpRedirect) {
        checkHttpMethod = false
        allowHttpsDowngrade = false
    }
    install(Logging) {
        level = LogLevel.INFO
    }
    defaultRequest {
        headers.append("User-Agent", "KoLmafia-Mobile/0.1")
    }
}
