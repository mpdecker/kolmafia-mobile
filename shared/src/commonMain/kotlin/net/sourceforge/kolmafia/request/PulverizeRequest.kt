package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.StoragePullRules

open class PulverizeRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val character: KoLCharacter? = null,
    private val junkListManager: JunkListManager? = null,
) {

    companion object {
        const val TENDER_HAMMER = 338

        private val ITEM_ID_PATTERN = Regex("""smashitem=(\d+)""")
        private val QTY_PATTERN = Regex("""(?:^|[?&])qty=(\d+)""")

        private val FAILURE_SIGNALS = listOf(
            "too important to pulverize",
            "not something you can pulverize",
            "don't know how to properly smash stuff",
            "haven't got that many",
        )

        fun parseResponse(urlString: String, responseText: String): Int {
            if (FAILURE_SIGNALS.any { responseText.contains(it, ignoreCase = true) }) {
                return 0
            }
            val itemId = ITEM_ID_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: return 0
            val qty = QTY_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: return 0
            if (itemId <= 0 || qty <= 0) return 0
            return qty
        }
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    open suspend fun pulverize(itemId: Int, quantity: Int): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (!EquipmentDatabase.isPulverizable(itemId)) {
            return Result.failure(IllegalArgumentException("Item $itemId is not pulverizable"))
        }

        retrieveItemService?.retrieve(TENDER_HAMMER, 1)
        if (inventoryCount(TENDER_HAMMER) <= 0) {
            return Result.success(0)
        }

        if (inventoryCount(itemId) < quantity) {
            retrieveItemService?.retrieve(itemId, quantity)
        }
        if (inventoryCount(itemId) < quantity) {
            return Result.success(0)
        }

        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/craft.php",
                formParameters = Parameters.build {
                    append("action", "pulverize")
                    append("smashitem", itemId.toString())
                    append("qty", quantity.toString())
                    append("ajax", "1")
                    append("conftrade", "1")
                },
            )
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val smashed = parseResponse(
                urlString = "craft.php?action=pulverize&smashitem=$itemId&qty=$quantity",
                responseText = body,
            )
            if (smashed <= 0) {
                return Result.success(0)
            }
            inventoryManager?.consumeItemLocally(itemId, smashed)
            inventoryManager?.fetchInventory()

            val charState = character?.state?.value
            if (charState != null &&
                !StoragePullRules.canInteract(charState) &&
                junkListManager != null &&
                !junkListManager.contains(itemId)
            ) {
                junkListManager.addToJunkList(itemId)
            }

            Result.success(smashed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
