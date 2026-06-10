package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class AutosellRequest(private val client: HttpClient) {
    open suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/sellstuff.php",
                formParameters = Parameters.build {
                    append("action", "sell")
                    append("ajax", "1")
                    append("type", "quant")
                    append("howmany", quantity.toString())
                    append("whichitem", itemId.toString())
                    append("quantity", quantity.toString())
                }
            )
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
