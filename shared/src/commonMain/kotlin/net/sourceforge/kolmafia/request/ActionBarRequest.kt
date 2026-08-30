package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.ActionBarManager

/** Server request for the headless action-bar state. */
open class ActionBarRequest(private val client: HttpClient) {
    open suspend fun fetch(): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/actionbar.php") {
            parameter("action", "fetch")
        }
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("HTTP ${response.status.value}"))
        } else {
            val json = response.bodyAsText()
            ActionBarManager.update(json)
            Result.success(json)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun set(json: String): Result<Unit> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/actionbar.php",
            formParameters = parameters {
                append("action", "set")
                append("bar", json)
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("HTTP ${response.status.value}"))
        } else {
            ActionBarManager.update(json)
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
