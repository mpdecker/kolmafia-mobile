package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HashingChoiceSync
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for using a hashing vise and submitting choice 1551. */
open class HashingViseRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val inventoryManager: InventoryManager? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    open suspend fun use(
        schematicItemId: Int,
        checksumItemId: Int? = null,
    ): Result<String> {
        if (schematicItemId <= 0) {
            return Result.failure(IllegalArgumentException("Schematic item ID must be positive."))
        }
        if (checksumItemId != null && checksumItemId <= 0) {
            return Result.failure(IllegalArgumentException("Checksum item ID must be positive."))
        }
        val inventory = inventoryManager
            ?: return Result.failure(IllegalStateException("Inventory is not available."))
        if (inventory.getCount(HASHING_VISE_ID) <= 0) {
            return Result.failure(IllegalStateException("You do not have a hashing vise to use."))
        }
        if (inventory.getCount(schematicItemId) <= 0) {
            return Result.failure(IllegalStateException("You do not have that schematic to hash."))
        }

        return try {
            val useResponse = client.submitForm(
                url = "$KOL_BASE_URL/inv_use.php",
                formParameters = parameters {
                    append("whichitem", HASHING_VISE_ID.toString())
                },
            )
            if (!useResponse.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("Hashing vise item-use request failed."),
                )
            }
            val useHtml = useResponse.bodyAsText()
            if (!isHashingChoice(useHtml)) {
                return Result.failure(
                    IllegalStateException("Using the hashing vise did not open choice 1551."),
                )
            }

            val choiceResult = choiceRequest.choose(
                HashingChoiceSync.CHOICE_ID,
                HASH_OPTION,
                mapOf("iid" to schematicItemId.toString()),
            )
            val (html, url) = choiceResult.getOrElse { return Result.failure(it) }
            val responseUrl = buildString {
                append(url)
                append(if (url.contains('?')) '&' else '?')
                append("whichchoice=${HashingChoiceSync.CHOICE_ID}")
                append("&option=$HASH_OPTION")
                append("&iid=$schematicItemId")
            }
            if (!parseResponse(responseUrl, html, schematicItemId, checksumItemId)) {
                return Result.failure(
                    IllegalStateException("Hashing vise choice response was not successful."),
                )
            }
            val schematicName = ItemDatabase.getItemName(schematicItemId)
                .ifEmpty { "item #$schematicItemId" }
            sessionLogger?.appendRawLine("vise $schematicName")
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseResponse(
        url: String,
        html: String,
        schematicItemId: Int,
        checksumItemId: Int? = null,
    ): Boolean {
        if (schematicItemId <= 0) return false
        val inventory = inventoryManager
        var parsedChecksum = false
        val handled = HashingChoiceSync.apply(
            choiceId = HashingChoiceSync.CHOICE_ID,
            html = html,
            choiceUrl = url,
            consumeItem = { itemId, quantity ->
                ResultProcessor.processItem(
                    itemId,
                    -quantity,
                    preferences = preferences,
                    inventory = inventory,
                )
            },
            gainItem = { itemId, quantity ->
                parsedChecksum = true
                ResultProcessor.processItem(
                    itemId,
                    quantity,
                    preferences = preferences,
                    inventory = inventory,
                )
            },
        )
        if (!handled) return false

        if (!parsedChecksum) {
            ResultProcessor.processResults(
                adventureResults = false,
                html = html,
                inventory = inventory,
                preferences = preferences,
            )
        }
        return true
    }

    private fun isHashingChoice(html: String): Boolean =
        Regex("""(?:whichchoice[^0-9]+1551|value\s*=\s*["']?1551)""", RegexOption.IGNORE_CASE)
            .containsMatchIn(html)

    companion object {
        const val HASHING_VISE_ID = 11826
        private const val HASH_OPTION = 1
    }
}
