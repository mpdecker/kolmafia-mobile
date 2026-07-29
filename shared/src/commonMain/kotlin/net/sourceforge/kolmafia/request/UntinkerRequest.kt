package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.coroutineScope
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager

open class UntinkerRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val gameDatabase: GameDatabase? = null,
    private val character: KoLCharacter? = null,
    private val adventureManager: AdventureManager? = null,
    private val goalManager: GoalManager? = null,
    private val questDatabase: QuestDatabase? = null,
) {

    companion object {
        private val ITEM_ID_PATTERN = Regex("""whichitem=(\d+)""")

        const val RUSTY_SCREWDRIVER = 454
        const val LOATHING_LEGION_SCREWDRIVER = 4926
        const val DEGRASSI_KNOLL_GARAGE_SNARFBLAT = "354"

        private var cachedCanUntinker: Boolean? = null

        internal fun resetForTest() {
            cachedCanUntinker = null
        }

        fun canUntinkerItem(itemName: String): Boolean {
            val concoction = ConcoctionDatabase.getByResult(itemName) ?: return false
            return concoction.isCombining || "JEWELRY" in concoction.methods
        }

        fun parseResponse(urlString: String, responseText: String, inventoryCount: Int): Int {
            if (!urlString.contains("fv_untinker")) return 0
            if (!responseText.contains("You acquire")) return 0

            val itemId = ITEM_ID_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull()
                ?: return 0
            if (itemId <= 0) return 0

            return if (urlString.contains("untinkerall=on")) {
                inventoryCount.coerceAtLeast(0)
            } else {
                1
            }
        }

        fun syncQuestFromResponse(
            urlString: String,
            responseText: String,
            inventoryHasScrewdriver: Boolean,
            questDatabase: QuestDatabase?,
            onScrewdriverRemoved: () -> Unit = {},
        ) {
            if (urlString.contains("screwquest") &&
                (responseText.contains("I'm just lost without my screwdriver") ||
                    responseText.contains("I'll go find your screwdriver for you"))
            ) {
                questDatabase?.setProgress(Quest.UNTINKER, QuestDatabase.STARTED)
            }

            if (urlString.contains("fv_untinker_quest")) {
                return
            }

            if (!urlString.contains("fv_untinker")) {
                return
            }

            if (inventoryHasScrewdriver) {
                onScrewdriverRemoved()
                questDatabase?.setProgress(Quest.UNTINKER, QuestDatabase.FINISHED)
            }
        }
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    private suspend fun probeUntinkerPage(): Pair<Boolean, String> {
        val response = client.get(
            "$KOL_BASE_URL/place.php?whichplace=forestvillage&action=fv_untinker",
        )
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            return false to body
        }
        syncQuestFromResponse(
            urlString = "place.php?whichplace=forestvillage&action=fv_untinker",
            responseText = body,
            inventoryHasScrewdriver = inventoryCount(RUSTY_SCREWDRIVER) > 0,
            questDatabase = questDatabase,
            onScrewdriverRemoved = { inventoryManager?.consumeItemLocally(RUSTY_SCREWDRIVER, 1) },
        )
        val available = body.contains("you don't have anything like that", ignoreCase = true) ||
            body.contains("<select name=whichitem>", ignoreCase = true)
        return available to body
    }

    open suspend fun canUntinker(): Boolean {
        cachedCanUntinker?.let { return it }

        return try {
            val (available, _) = probeUntinkerPage()
            cachedCanUntinker = available
            available
        } catch (_: Exception) {
            cachedCanUntinker = false
            false
        }
    }

    private suspend fun postScrewquest(): String {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/place.php",
            formParameters = Parameters.build {
                append("whichplace", "forestvillage")
                append("action", "fv_untinker_quest")
                append("preaction", "screwquest")
            },
        )
        val body = response.bodyAsText()
        syncQuestFromResponse(
            urlString = "place.php?preaction=screwquest",
            responseText = body,
            inventoryHasScrewdriver = false,
            questDatabase = questDatabase,
        )
        return body
    }

    open suspend fun completeQuest(): Boolean {
        return try {
            postScrewquest()
            cachedCanUntinker = null

            if (character?.state?.value?.knollAvailable == true) {
                client.get("$KOL_BASE_URL/place.php") {
                    parameter("whichplace", "knoll_friendly")
                    parameter("action", "dk_innabox")
                }
                return canUntinker()
            }

            val manager = adventureManager
            val goals = goalManager
            if (manager == null || goals == null) {
                return false
            }

            val zone = AdventureDatabase.getBySnarfblat(DEGRASSI_KNOLL_GARAGE_SNARFBLAT)
            val location = AdventureLocation(
                id = DEGRASSI_KNOLL_GARAGE_SNARFBLAT,
                name = zone?.locationName ?: "The Degrassi Knoll Garage",
                zone = zone?.zoneName ?: "Degrassi Knoll",
            )
            val maxTurns = character?.state?.value?.adventuresLeft ?: 0

            val obtained = coroutineScope {
                goals.runSideTripForItem(
                    adventureManager = manager,
                    location = location,
                    itemId = RUSTY_SCREWDRIVER,
                    maxTurns = maxTurns,
                    scope = this,
                    itemCount = { inventoryCount(RUSTY_SCREWDRIVER) },
                )
            }

            cachedCanUntinker = null
            if (obtained) {
                return canUntinker()
            }
            val (_, body) = probeUntinkerPage()
            !body.contains("Degrassi Knoll", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    open suspend fun completeQuestKnoll(): Boolean {
        if (character?.state?.value?.knollAvailable != true) {
            return false
        }
        return completeQuest()
    }

    private suspend fun executeUntinkerOnce(itemId: Int, untinkerAll: Boolean): Pair<Int, String> {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/place.php",
            formParameters = Parameters.build {
                append("whichplace", "forestvillage")
                append("action", "fv_untinker")
                append("preaction", "untinker")
                append("whichitem", itemId.toString())
                if (untinkerAll) {
                    append("untinkerall", "on")
                }
            },
        )
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception("HTTP ${response.status.value}")
        }

        val urlString = buildString {
            append("place.php?whichplace=forestvillage&action=fv_untinker&whichitem=$itemId")
            if (untinkerAll) append("&untinkerall=on")
        }
        syncQuestFromResponse(
            urlString = urlString,
            responseText = body,
            inventoryHasScrewdriver = inventoryCount(RUSTY_SCREWDRIVER) > 0,
            questDatabase = questDatabase,
            onScrewdriverRemoved = { inventoryManager?.consumeItemLocally(RUSTY_SCREWDRIVER, 1) },
        )

        val beforeCount = inventoryCount(itemId)
        val untinkered = parseResponse(urlString, body, beforeCount)
        if (untinkered > 0) {
            inventoryManager?.consumeItemLocally(itemId, untinkered)
        }
        return untinkered to body
    }

    open suspend fun untinker(itemId: Int, quantity: Int): Result<Int> {
        if (itemId <= 0 || quantity <= 0) return Result.success(0)

        val itemName = gameDatabase?.item(itemId)?.name
            ?: return Result.failure(IllegalArgumentException("Unknown item id $itemId"))
        if (!canUntinkerItem(itemName)) {
            return Result.failure(IllegalArgumentException("Item $itemId cannot be untinkered"))
        }

        retrieveItemService?.retrieve(itemId, quantity)
        val available = inventoryCount(itemId)
        if (available <= 0) {
            return Result.success(0)
        }

        val effectiveQty = quantity.coerceAtMost(available)
        val untinkerAll = effectiveQty > 5 || available == effectiveQty
        val iterations = if (untinkerAll) 1 else effectiveQty

        return try {
            var totalUntinkered = 0
            repeat(iterations) { index ->
                val (untinkered, body) = executeUntinkerOnce(itemId, untinkerAll)
                if (untinkered <= 0 && index == 0 && !body.contains("You acquire")) {
                    cachedCanUntinker = null
                    val (_, probeBody) = probeUntinkerPage()
                    if (!probeBody.contains("<select", ignoreCase = true)) {
                        if (!completeQuest()) {
                            return Result.success(totalUntinkered)
                        }
                        cachedCanUntinker = null
                        probeUntinkerPage()
                        val (retryUntinkered, _) = executeUntinkerOnce(itemId, untinkerAll)
                        if (retryUntinkered <= 0) {
                            return Result.success(totalUntinkered)
                        }
                        totalUntinkered += retryUntinkered
                        return@repeat
                    }
                    return Result.success(totalUntinkered)
                }
                if (untinkered <= 0) {
                    return Result.success(totalUntinkered)
                }
                totalUntinkered += untinkered
            }
            inventoryManager?.fetchInventory()
            Result.success(totalUntinkered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun untinkerViaLegionScrewdriver(itemId: Int, quantity: Int): Result<Int> {
        if (itemId <= 0 || quantity <= 0) return Result.success(0)

        val itemName = gameDatabase?.item(itemId)?.name
            ?: return Result.failure(IllegalArgumentException("Unknown item id $itemId"))
        if (!canUntinkerItem(itemName)) {
            return Result.failure(IllegalArgumentException("Item $itemId cannot be untinkered"))
        }

        retrieveItemService?.retrieve(LOATHING_LEGION_SCREWDRIVER, 1)
        retrieveItemService?.retrieve(itemId, quantity)

        val available = inventoryCount(itemId)
        if (available <= 0) {
            return Result.success(0)
        }

        val effectiveQty = quantity.coerceAtMost(available)
        val untinkerAll = effectiveQty > 5 || available == effectiveQty
        val iterations = if (untinkerAll) 1 else effectiveQty

        return try {
            var totalUntinkered = 0
            repeat(iterations) {
                val response = client.get("$KOL_BASE_URL/inv_use.php") {
                    parameter("ajax", "1")
                    parameter("whichitem", LOATHING_LEGION_SCREWDRIVER.toString())
                    parameter("action", "screw")
                    parameter("dowhichitem", itemId.toString())
                    if (untinkerAll) {
                        parameter("untinkerall", "on")
                    }
                }
                val body = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    return Result.failure(Exception("HTTP ${response.status.value}"))
                }

                val urlString = buildString {
                    append(
                        "inv_use.php?whichitem=$LOATHING_LEGION_SCREWDRIVER&action=screw&dowhichitem=$itemId",
                    )
                    if (untinkerAll) append("&untinkerall=on")
                }
                val beforeCount = inventoryCount(itemId)
                val untinkered = if (body.contains("You acquire")) {
                    if (untinkerAll) beforeCount.coerceAtLeast(0) else 1
                } else {
                    0
                }
                if (untinkered <= 0) {
                    return Result.success(totalUntinkered)
                }
                inventoryManager?.consumeItemLocally(itemId, untinkered)
                totalUntinkered += untinkered
            }
            inventoryManager?.fetchInventory()
            Result.success(totalUntinkered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
