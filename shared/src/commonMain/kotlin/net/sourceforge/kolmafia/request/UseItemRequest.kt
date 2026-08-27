package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ProtonicGhostSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemUsedSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.DreadScrollManager
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.recovery.BetweenBattleInvoker

open class UseItemRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
    private val eventBus: GameEventBus? = null,
    private val questDatabase: QuestDatabase? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
) {
    /**
     * Uses an item via inv_use.php.
     * @param itemId  KoL item ID
     * @param quantity  number to use (default 1)
     */
    open suspend fun use(itemId: Int, quantity: Int = 1): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        return try {
            val response = client.get("$KOL_BASE_URL/inv_use.php") {
                parameter("which", 3)
                parameter("whichitem", itemId)
                parameter("ajax", 1)
                if (quantity > 1) parameter("quantity", quantity)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                UseItemConsumptionSync.rememberLastItem(itemId, quantity)
                if (itemId == DreadScrollManager.KNUCKLEBONE_ID) {
                    DreadScrollManager.handleKnucklebone(body, preferences, sessionLogger)
                } else if (itemId == DreadScrollManager.DREADSCROLL_ID) {
                    DreadScrollManager.parseDreadscrollUse(body, preferences, eventBus, sessionLogger)
                } else if (itemId == ProtonicGhostSync.WALKIE_TALKIE) {
                    ProtonicGhostSync.applyFromWalkieTalkie(
                        html = body,
                        questDatabase = questDatabase,
                        preferences = preferences,
                        turnsPlayed = character?.state?.value?.turnsPlayed ?: 0,
                    )
                } else {
                    val questHandled = QuestItemUsedSync.apply(
                        itemId,
                        body,
                        questDatabase,
                        preferences,
                        consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
                        count = quantity,
                    )
                    UseItemConsumptionSync.parseConsumption(
                        responseText = body,
                        itemId = itemId,
                        count = quantity,
                        preferences = preferences,
                        character = character,
                        inventory = if (questHandled) null else inventoryManager,
                    )
                }
                BetweenBattleInvoker.run(true)
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop MultiUseRequest — multi-use an ingredient stack via multiuse.php. */
    open suspend fun multiUse(itemId: Int, quantity: Int): Result<String> {
        if (quantity <= 0) return Result.success("")
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/multiuse.php",
                formParameters = parameters {
                    append("action", "useitem")
                    append("whichitem", itemId.toString())
                    append("quantity", quantity.toString())
                },
            )
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                UseItemConsumptionSync.rememberLastItem(itemId, quantity)
                QuestItemUsedSync.apply(
                    itemId,
                    body,
                    questDatabase,
                    preferences,
                    consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
                    count = quantity,
                )
                UseItemConsumptionSync.parseConsumption(
                    responseText = body,
                    itemId = itemId,
                    count = quantity,
                    preferences = preferences,
                    character = character,
                    inventory = inventoryManager,
                )
                BetweenBattleInvoker.run(true)
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest GLUTTONOUS_GHOST / SPIRIT_HOBO / SLIMELING binge via familiarbinger.php. */
    open suspend fun binge(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/familiarbinger.php") {
                parameter("whichitem", itemId)
                parameter("action", "binge")
                parameter("qty", quantity)
            }
            parseFamiliarFeedResponse(response.status.isSuccess(), response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest STOCKING_MIMIC candy feed via familiarbinger.php. */
    open suspend fun feedCandy(itemId: Int, quantity: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/familiarbinger.php") {
                parameter("whichitem", itemId)
                parameter("action", "candy")
                parameter("qty", quantity)
            }
            parseFamiliarFeedResponse(response.status.isSuccess(), response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop UseItemRequest ROBORTENDER robooze via inventory.php (qty 1 per call). */
    open suspend fun robooze(itemId: Int): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inventory.php") {
                parameter("action", "robooze")
                parameter("whichitem", itemId)
                parameter("ajax", 1)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }
            val body = response.bodyAsText()
            if (body.contains("can't drink that", ignoreCase = true)) {
                Result.failure(IllegalStateException("Your Robortender can't drink that."))
            } else {
                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFamiliarFeedResponse(httpSuccess: Boolean, body: String): Result<String> {
        if (!httpSuccess) {
            return Result.failure(Exception("HTTP request failed"))
        }
        if (body.contains("don't currently have", ignoreCase = true) ||
            body.contains("not currently using", ignoreCase = true)
        ) {
            return Result.failure(IllegalStateException("Your current familiar can't use that."))
        }
        return Result.success(body)
    }
}
