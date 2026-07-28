package net.sourceforge.kolmafia.npc

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.NpcShopSync

open class NpcBuyRequest(private val client: HttpClient) {

    open suspend fun visitStore(
        storeKey: String,
        prefs: Preferences?,
        ascensionNumber: Int,
    ): Result<String> {
        if (prefs == null || !NpcShopSync.needsSync(storeKey)) {
            return Result.success("")
        }
        return try {
            val response = client.get("$KOL_BASE_URL/store.php") {
                parameter("whichstore", storeKey)
            }
            val body = response.bodyAsText()
            val url = "$KOL_BASE_URL/store.php?whichstore=$storeKey"
            NpcShopSync.syncFromStoreHtml(storeKey, body, prefs, ascensionNumber, url)
            Result.success(body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun buy(
        storeKey: String,
        itemId: Int,
        quantity: Int,
        prefs: Preferences? = null,
    ): Result<Int> = try {
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
            Result.success(0)
        } else {
            if (quantity > 0 && prefs != null) {
                NpcShopSync.applyWildfirePurchase(
                    body,
                    "$KOL_BASE_URL/store.php?whichstore=$storeKey",
                    itemId,
                    prefs,
                )
            }
            Result.success(quantity)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
