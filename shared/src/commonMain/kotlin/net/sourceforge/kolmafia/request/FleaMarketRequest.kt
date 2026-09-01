package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
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
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for buying from the Flea Market (`town_fleamarket.php`). */
open class FleaMarketRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager?,
    private val character: KoLCharacter?,
    private val sessionLogger: SessionLogger?,
    private val preferences: Preferences? = null,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

    open suspend fun buy(itemId: Int, quantity: Int): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(
                IllegalStateException(
                    RequestAbortGate.lastAbortMessage.ifEmpty {
                        "You are currently in a fight or choice."
                    },
                ),
            )
        }
        if (quantity <= 0 || itemId <= 0 || ItemDatabase.getItemName(itemId).isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Flea Market buy requires a resolved item and positive quantity."),
            )
        }
        return try {
            var lastHtml = ""
            repeat(quantity) {
                handledSignatures.clear()
                val listingResponse = client.get("$KOL_BASE_URL/$PAGE")
                if (!listingResponse.status.isSuccess()) {
                    throw IllegalStateException("Flea Market listing request failed.")
                }
                val listing = parseListing(listingResponse.bodyAsText(), itemId)
                    ?: throw IllegalStateException("That item is not listed at the Flea Market.")
                val url = buyUrl(listing.which, itemId, listing.howmuch)
                val response = client.submitForm(
                    url = "$KOL_BASE_URL/$PAGE",
                    formParameters = parameters {
                        append("buying", BUYING)
                        append("which", listing.which.toString())
                        append("whichitem", itemId.toString())
                        append("howmuch", listing.howmuch.toString())
                    },
                )
                if (!response.status.isSuccess()) {
                    throw IllegalStateException("Flea Market buy request failed.")
                }
                lastHtml = response.bodyAsText()
                if (!parseResponse(url, lastHtml)) {
                    throw IllegalStateException("Flea Market buy response was not successful.")
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
        ResultProcessor.processResults(
            adventureResults = false,
            html = html,
            inventory = inventoryManager,
            character = character,
            preferences = preferences,
        )
        handledSignatures += signature
        return true
    }

    companion object {
        const val PAGE = "town_fleamarket.php"
        const val BUYING = "Yep."

        private val PURCHASED = Regex(
            """You purchase the item from (.*? \( #\d+ \))""",
            RegexOption.IGNORE_CASE,
        )
        private val LISTING = Regex(
            """name=which\s+value=["']?(\d+)["']?[\s\S]{0,300}?name=whichitem\s+value=["']?(\d+)["']?[\s\S]{0,300}?name=howmuch\s+value=["']?(\d+)["']?""",
            RegexOption.IGNORE_CASE,
        )

        fun isBuyUrl(url: String): Boolean =
            url.contains(PAGE, ignoreCase = true)

        fun registerRequest(urlString: String, sessionLogger: SessionLogger?): Boolean {
            if (!isBuyUrl(urlString)) return false
            val itemId = queryInt(urlString, "whichitem") ?: return false
            if (itemId < 0) return false
            val itemName = ItemDatabase.getItemName(itemId)
            if (itemName.isEmpty()) return false
            val price = queryInt(urlString, "howmuch") ?: 0
            RequestLogger.updateSessionLog(
                "Purchasing $itemName from the Flea Market for $price meat.",
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
            if (!isBuyUrl(url)) return false
            val itemId = queryInt(url, "whichitem") ?: return false
            if (itemId < 0) return false
            val itemName = ItemDatabase.getItemName(itemId)
            if (itemName.isEmpty()) return false
            val price = queryInt(url, "howmuch") ?: return false
            val purchased = PURCHASED.find(html) ?: return false
            if (price > 0) {
                ResultProcessor.processMeat(-price.toLong(), character)
            }
            RequestLogger.updateSessionLog(
                "Purchased $itemName from ${purchased.groupValues[1]} at the Flea Market for $price meat.",
                sessionLogger,
            )
            return true
        }

        internal fun parseListing(html: String, itemId: Int): FleaListing? {
            LISTING.findAll(html).forEach { match ->
                val listedId = match.groupValues[2].toIntOrNull() ?: return@forEach
                if (listedId != itemId) return@forEach
                val which = match.groupValues[1].toIntOrNull() ?: return@forEach
                val howmuch = match.groupValues[3].toIntOrNull() ?: return@forEach
                return FleaListing(which = which, howmuch = howmuch)
            }
            return null
        }

        internal fun buyUrl(which: Int, itemId: Int, howmuch: Int): String =
            "$PAGE?buying=$BUYING&which=$which&whichitem=$itemId&howmuch=$howmuch"

        internal fun queryInt(url: String, key: String): Int? =
            Regex("""(?:^|[?&])$key=([^&]*)""", RegexOption.IGNORE_CASE)
                .find(url)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
    }
}

internal data class FleaListing(val which: Int, val howmuch: Int)
