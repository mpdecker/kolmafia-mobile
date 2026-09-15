package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Wraps hermit.php trade requests.
 * POST hermit.php?action=trade&whichitem=ID&quantity=N
 */
open class HermitRequest(private val client: HttpClient) {

    companion object {
        const val ELEVEN_LEAF_CLOVER_ID = 10881
        const val WORTHLESS_ITEM_ID = 13
        const val WORTHLESS_TRINKET_ID = 43
        const val WORTHLESS_GEWGAW_ID = 44
        const val WORTHLESS_KNICK_KNACK_ID = 45
        private val WORTHLESS_COMPONENT_IDS = setOf(
            WORTHLESS_TRINKET_ID,
            WORTHLESS_GEWGAW_ID,
            WORTHLESS_KNICK_KNACK_ID,
        )
        private val CLOVER_STOCK_PATTERN = Regex("""(\d+)\s+left in stock for today""")
        private val TRADABLE_ITEMS_PATTERN =
            Regex("""You have ([\d,]+) tradable items""")
        private val QUANTITY_URL_PATTERN = Regex("""[?&]quantity=(\d+)""", RegexOption.IGNORE_CASE)
        private val CLOVER_ACQUIRE_PATTERN = Regex(
            """You acquire an item:\s*<b>([\d,]+)\s+eleven-leaf clover""",
            RegexOption.IGNORE_CASE,
        )

        /** Desktop [HermitRequest.parseHermitTrade] / [parseHermitStock] visit HTML sync. */
        fun parseResponse(
            url: String,
            html: String,
            preferences: Preferences?,
            @Suppress("UNUSED_PARAMETER") inventory: InventoryManager? = null,
        ) {
            if (!url.contains("hermit.php", ignoreCase = true)) return
            val prefs = preferences ?: return

            TRADABLE_ITEMS_PATTERN.find(html)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.toIntOrNull()
                ?.let { prefs.setInt("hermitTradableItems", it) }

            val cloverStock = CLOVER_STOCK_PATTERN.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            prefs.setInt("hermitCloverCount", cloverStock)

            if (html.contains("You acquire", ignoreCase = true)) {
                val fromUrl = if (url.contains("whichitem=$ELEVEN_LEAF_CLOVER_ID", ignoreCase = true)) {
                    QUANTITY_URL_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                } else {
                    0
                }
                val fromHtml = CLOVER_ACQUIRE_PATTERN.find(html)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")
                    ?.toIntOrNull()
                    ?: 0
                val acquired = maxOf(fromUrl, fromHtml)
                if (acquired > 0) {
                    prefs.increment("_cloversPurchased", acquired)
                }
            }

        }

        /** Desktop [HermitRequest.getAvailableWorthlessItemCount]. */
        suspend fun availableWorthlessItemCount(
            inventoryManager: net.sourceforge.kolmafia.inventory.InventoryManager?,
            closetRequest: ClosetRequest?,
            storageRequest: StorageRequest?,
        ): Int {
            val inventory = inventoryManager?.state?.value?.items.orEmpty()
            val closet = closetRequest?.fetchContents().orEmpty()
            val storage = storageRequest?.fetchContents().orEmpty()
            return worthlessCountFromMaps(
                inventory.keys.associateWith { inventory[it]?.quantity ?: 0 },
                closet,
                storage,
            )
        }

        fun worthlessCountFromMaps(
            inventory: Map<Int, Int>,
            closet: Map<Int, Int>,
            storage: Map<Int, Int>,
        ): Int = WORTHLESS_COMPONENT_IDS.sumOf { id ->
            (inventory[id] ?: 0) + (closet[id] ?: 0) + (storage[id] ?: 0)
        }

        fun parseCloverCount(html: String): Int =
            CLOVER_STOCK_PATTERN.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    open suspend fun trade(itemId: Int, quantity: Int): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/hermit.php",
            formParameters = Parameters.build {
                append("action", "trade")
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

    /** Desktop [HermitRequest.cloverCount] — eleven-leaf clovers left at Hermit today. */
    open suspend fun fetchCloverCount(
        ascensionPath: AscensionPath = AscensionPath.NONE,
        preferences: Preferences? = null,
    ): Int {
        if (ascensionPath == AscensionPath.ZOMBIE_SLAYER) {
            val used = preferences?.getBoolean("_zombieClover0", false) ?: false
            return if (used) 0 else 1
        }
        return try {
            val html = client.get("$KOL_BASE_URL/hermit.php").bodyAsText()
            parseCloverCount(html)
        } catch (_: Exception) {
            0
        }
    }

    fun parseCloverCount(html: String): Int = Companion.parseCloverCount(html)
}
