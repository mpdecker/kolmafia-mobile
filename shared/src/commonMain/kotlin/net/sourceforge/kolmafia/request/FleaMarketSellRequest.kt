package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for listing an item at the Flea Market (`town_sellflea.php`). */
open class FleaMarketSellRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager?,
    private val character: KoLCharacter?,
    private val sessionLogger: SessionLogger?,
    private val preferences: Preferences? = null,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

    open suspend fun sell(itemId: Int, quantity: Int, price: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(
                IllegalStateException(
                    RequestAbortGate.lastAbortMessage.ifEmpty {
                        "You are currently in a fight or choice."
                    },
                ),
            )
        }
        if (quantity <= 0 || price <= 0 || itemId <= 0 || ItemDatabase.getItemName(itemId).isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Flea Market sell requires a resolved item, positive quantity, and positive price.",
                ),
            )
        }
        val owned = inventoryManager?.getCount(itemId) ?: 0
        if (owned < quantity) {
            return Result.failure(
                IllegalStateException("You don't have that many to sell at the Flea Market."),
            )
        }
        return try {
            var lastHtml = ""
            repeat(quantity) {
                handledSignatures.clear()
                val url = sellUrl(itemId, price)
                val response = client.submitForm(
                    url = "$KOL_BASE_URL/$PAGE",
                    formParameters = parameters {
                        append("whichitem", itemId.toString())
                        append("sellprice", price.toString())
                        append("selling", SELLING)
                    },
                )
                if (!response.status.isSuccess()) {
                    throw IllegalStateException("Flea Market sell request failed.")
                }
                lastHtml = response.bodyAsText()
                if (!parseResponse(url, lastHtml)) {
                    throw IllegalStateException("Flea Market sell response was not successful.")
                }
            }
            Result.success(lastHtml)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseResponse(url: String, html: String): Boolean {
        val signature = url to html
        if (signature in handledSignatures) return true
        val handled = parseResponse(url, html, inventoryManager, character, sessionLogger)
        if (!handled) return false
        handledSignatures += signature
        return true
    }

    companion object {
        const val PAGE = "town_sellflea.php"
        const val SELLING = "Yep."
        const val SUCCESS = "You place your item for sale in the Flea Market."

        fun isSellUrl(url: String): Boolean =
            url.contains(PAGE, ignoreCase = true)

        fun registerRequest(urlString: String, sessionLogger: SessionLogger?): Boolean {
            if (!isSellUrl(urlString)) return false
            val itemId = queryInt(urlString, "whichitem") ?: return false
            if (itemId < 0) return false
            val itemName = ItemDatabase.getItemName(itemId)
            if (itemName.isEmpty()) return false
            val price = queryInt(urlString, "sellprice") ?: 0
            RequestLogger.updateSessionLog(
                "Placing $itemName up for sale at the Flea Market for $price meat.",
                sessionLogger,
            )
            return true
        }

        fun parseResponse(
            url: String,
            html: String,
            inventory: InventoryManager?,
            character: KoLCharacter?,
            sessionLogger: SessionLogger?,
        ): Boolean {
            if (!isSellUrl(url)) return false
            val itemId = queryInt(url, "whichitem") ?: return false
            if (itemId < 0) return false
            val itemName = ItemDatabase.getItemName(itemId)
            if (itemName.isEmpty()) return false
            if (!html.contains(SUCCESS, ignoreCase = true)) return false
            val price = queryInt(url, "sellprice") ?: 0
            inventory?.consumeItemLocally(itemId, 1)
            RequestLogger.updateSessionLog(
                "Placed $itemName up for sale at the Flea Market for $price meat.",
                sessionLogger,
            )
            return true
        }

        internal fun sellUrl(itemId: Int, price: Int): String =
            "$PAGE?whichitem=$itemId&sellprice=$price&selling=$SELLING"

        private fun queryInt(url: String, key: String): Int? =
            Regex("""(?:^|[?&])$key=([^&]*)""", RegexOption.IGNORE_CASE)
                .find(url)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
    }
}
