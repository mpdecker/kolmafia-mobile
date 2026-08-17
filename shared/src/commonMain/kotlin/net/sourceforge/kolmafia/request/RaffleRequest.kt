package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop [RaffleRequest] — Desert Beach raffle ticket purchase. */
open class RaffleRequest(private val client: HttpClient) {

    enum class RaffleSource(val where: String) {
        INVENTORY("0"),
        STORAGE("1"),
    }

    open suspend fun buy(quantity: Int, source: RaffleSource): Result<String> {
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/raffle.php",
                formParameters = parameters {
                    append("action", "buy")
                    append("where", source.where)
                    append("quantity", quantity.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("HTTP ${response.status.value}"))
            }
            val html = response.bodyAsText()
            when {
                html.contains("You cannot afford") -> {
                    val loc = if (source == RaffleSource.INVENTORY) "inventory" else "storage"
                    Result.failure(IllegalStateException("You don't have enough meat in $loc"))
                }
                !html.contains("Here you go") ->
                    Result.failure(IllegalStateException("Ticket purchase failed"))
                else -> Result.success(html)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
