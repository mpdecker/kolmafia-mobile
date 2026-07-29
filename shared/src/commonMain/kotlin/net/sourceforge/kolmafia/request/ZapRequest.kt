package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.WandDiscovery

open class ZapRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val useItemRequest: UseItemRequest? = null,
) {

    data class ZapParseResult(
        val success: Boolean = false,
        val consumedItemId: Int = 0,
        val acquiredItemName: String? = null,
        val wandExploded: Boolean = false,
    )

    companion object {
        private val ZAP_PATTERN = Regex("""whichitem=(\d+)""")
        private val ACQUIRE_PATTERN = Regex("""You acquire an item:\s*<b>(.*?)</b>""")

        fun parseResponse(urlString: String, responseText: String): ZapParseResult {
            if (!urlString.startsWith("wand.php")) {
                return ZapParseResult()
            }
            if (responseText.contains("nothing happens")) {
                return ZapParseResult()
            }

            val itemId = ZAP_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull()
                ?: return ZapParseResult()
            val acquired = ACQUIRE_PATTERN.find(responseText)?.groupValues?.get(1)

            return ZapParseResult(
                success = true,
                consumedItemId = itemId,
                acquiredItemName = acquired,
                wandExploded = responseText.contains("abruptly explodes"),
            )
        }
    }

    open suspend fun zap(itemId: Int): Result<Int> {
        if (itemId <= 0) {
            return Result.success(-1)
        }

        val ascensionNumber = character?.state?.value?.ascensionNumber ?: 0
        val wandId = WandDiscovery.getZapper(
            inventoryManager = inventoryManager,
            preferences = preferences,
            ascensionNumber = ascensionNumber,
            useItemRequest = useItemRequest,
        ) ?: return Result.success(-1)

        if (!WandDiscovery.hasItem(inventoryManager, itemId)) {
            retrieveItemService?.retrieve(itemId, 1)
        }
        if (!WandDiscovery.hasItem(inventoryManager, itemId)) {
            return Result.success(-1)
        }

        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/wand.php",
                formParameters = Parameters.build {
                    append("action", "zap")
                    append("whichwand", wandId.toString())
                    append("whichitem", itemId.toString())
                },
            )
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }

            val urlString = "wand.php?action=zap&whichwand=$wandId&whichitem=$itemId"
            val parsed = parseResponse(urlString, body)
            if (!parsed.success) {
                return Result.success(-1)
            }

            applyParseResult(parsed, wandId)
            inventoryManager?.fetchInventory()

            val acquiredId = parsed.acquiredItemName
                ?.let { ItemDatabase.getByName(it)?.id }
                ?: -1
            Result.success(acquiredId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyParseResult(result: ZapParseResult, wandId: Int) {
        if (result.wandExploded) {
            inventoryManager?.consumeItemLocally(wandId, 1)
            preferences?.setInt("_zapCount", -1)
            val dayCount = character?.state?.value?.dayCount ?: 0
            preferences?.setInt("lastZapperWandExplosionDay", dayCount)
        }

        if (result.consumedItemId > 0) {
            inventoryManager?.consumeItemLocally(result.consumedItemId, 1)
        }

        preferences?.let { prefs ->
            prefs.setInt("_zapCount", prefs.getInt("_zapCount", 0) + 1)
        }
    }
}
