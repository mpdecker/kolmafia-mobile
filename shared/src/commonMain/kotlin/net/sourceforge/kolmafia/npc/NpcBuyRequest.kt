package net.sourceforge.kolmafia.npc

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class NpcBuyRequest(private val client: HttpClient) {

    open suspend fun buy(storeKey: String, itemId: Int, quantity: Int): Result<Int> = runCatching {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/store.php",
            formParameters = parameters {
                append("whichstore", storeKey)
                append("buying", "1")
                append("whichitem", itemId.toString())
                append("howmany", quantity.toString())
                append("ajax", "1")
            }
        )
        val body = response.bodyAsText()
        if (body.contains("You can't afford") || body.contains("That store doesn't")) {
            return@runCatching 0
        }
        quantity
    }
}
