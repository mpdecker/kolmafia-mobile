package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ConsumptionHelperState
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

open class EatFoodRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    open suspend fun eat(itemId: Int, quantity: Int = 1): Result<String> =
        consumeFood(itemId, quantity).fold(
            onSuccess = { outcome ->
                when (outcome) {
                    is ConsumptionRequestOutcome.Completed -> Result.success("")
                    is ConsumptionRequestOutcome.Aborted ->
                        Result.failure(IllegalStateException(outcome.reason))
                }
            },
            onFailure = { Result.failure(it) },
        )

    suspend fun consumeFood(itemId: Int, quantity: Int = 1): Result<ConsumptionRequestOutcome> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        if (quantity <= 0) {
            return Result.success(ConsumptionRequestOutcome.Completed(0))
        }

        val iterations = iterationCount(itemId, quantity)
        var totalConsumed = 0

        for (iteration in 1..iterations) {
            ConsumptionHelperState.beginIteration(QueueBucket.FOOD, iteration)
            val iterQty = if (iterations > 1) 1 else quantity
            val utensil = ConsumptionHelperState.utensilForEat()

            val httpResult = performEat(itemId, iterQty, utensil)
            httpResult.exceptionOrNull()?.let { return Result.failure(it) }

            val body = httpResult.getOrThrow()
            UseItemConsumptionSync.rememberLastItem(itemId, iterQty)
            if (isEatAbort(body)) {
                UseItemConsumptionSync.clearLastItem()
                return Result.success(
                    ConsumptionRequestOutcome.Aborted(totalConsumed, eatAbortReason(body)),
                )
            }
            if (!UseItemConsumptionSync.parseConsumption(
                    responseText = body,
                    itemId = itemId,
                    count = iterQty,
                    preferences = preferences,
                    character = character,
                    inventory = inventoryManager,
                )
            ) {
                return Result.success(
                    ConsumptionRequestOutcome.Aborted(
                        totalConsumed,
                        UseItemConsumptionSync.lastUpdate.ifBlank { eatAbortReason(body) },
                    ),
                )
            }

            totalConsumed += iterQty
            if (utensil != null) {
                ConsumptionHelperState.decrementFoodHelper()
            }
        }

        ConsumptionHelperState.markFullyConsumed(QueueBucket.FOOD, totalConsumed)
        return Result.success(ConsumptionRequestOutcome.Completed(totalConsumed))
    }

    fun queueFoodHelper(itemId: Int, quantity: Int): Result<Unit> {
        ConsumptionHelperState.queueFoodHelper(itemId, quantity)
        return Result.success(Unit)
    }

    private suspend fun performEat(itemId: Int, quantity: Int, utensilId: Int?): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inv_eat.php") {
                parameter("which", 1)
                parameter("whichitem", itemId)
                parameter("ajax", 1)
                if (quantity > 1) parameter("quantity", quantity)
                utensilId?.let { parameter("utensil", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun iterationCount(itemId: Int, quantity: Int): Int {
        if (quantity <= 1) return 1
        if (singleConsume(itemId)) return quantity
        return 1
    }

    private fun singleConsume(itemId: Int): Boolean {
        if (ConsumptionHelperState.currentFoodHelper() != null) return true
        return itemId == BLACK_PUDDING || itemId == SMORE
    }

    companion object {
        private const val BLACK_PUDDING = 2338
        private const val SMORE = 5071
        const val GRAINS_OF_SALT = 6672
        const val JAR_OF_SWAMP_HONEY = 8226
        const val DRY_RUB = 7553
        const val SPECIAL_SEASONING = 9924
        private val MAYONEX_PATTERN =
            Regex("""Force of Mayo Be With You</b><br>\(duration: (\d+) Adventure""")

        /**
         * Desktop [EatItemRequest.handleFoodHelper] — helper inventory/prefs after a successful eat.
         * [adjustFullness] is false when [UseItemConsumptionSync.parseEat] already applied organs.
         */
        fun handleFoodHelper(
            itemName: String,
            count: Int,
            responseText: String,
            preferences: Preferences?,
            inventory: InventoryManager? = ResultProcessor.inventoryProvider?.invoke(),
            character: KoLCharacter? = null,
            adjustFullness: Boolean = true,
        ) {
            val qty = count.coerceAtLeast(1)
            if (responseText.contains("You chase it with that salt you made")) {
                val remaining = 3 - (preferences?.getInt("_saltGrainsConsumed", 0) ?: 0)
                val used = minOf(qty, inventory?.getCount(GRAINS_OF_SALT) ?: qty, remaining.coerceAtLeast(0))
                if (used > 0) {
                    ResultProcessor.processItem(GRAINS_OF_SALT, -used, preferences, inventory = inventory)
                    preferences?.increment("_saltGrainsConsumed", used)
                }
            }
            if (responseText.contains("in swamp honey before you eat it.")) {
                consumeHelper(JAR_OF_SWAMP_HONEY, qty, inventory, preferences)
            }
            if (responseText.contains("a nice dry rubbing before going to work on it")) {
                consumeHelper(DRY_RUB, qty, inventory, preferences)
            }
            if (responseText.contains("packet of your Special Seasoning")) {
                consumeHelper(SPECIAL_SEASONING, qty, inventory, preferences)
            }
            if (responseText.contains("With your sharpened appetite")) {
                decrementPref(preferences, "whetstonesUsed", qty)
            }
            if (responseText.contains("that fatty kiwi flavor")) {
                decrementPref(preferences, "miniKiwiAiolisUsed", qty)
            }
            if (responseText.contains("festive Christmas jelly")) {
                preferences?.setBoolean("_infiniteJellyUsed", true)
            }
            if (responseText.contains("magnesium-flavored belch")) {
                preferences?.setBoolean("milkOfMagnesiumActive", false)
            }
            if (character?.state?.value?.isPastamancer == true) {
                if (responseText.contains("feel suddenly bloated")) {
                    preferences?.setInt("carboLoading", 0)
                }
                if (responseText.contains("Mmm, this tastes a little bit spicier")) {
                    preferences?.setBoolean("_legendarySpiceGhostFood", true)
                }
            }
            if (responseText.contains("reminding you to squirt some mayonnaise")) {
                preferences?.increment("mayoLevel", qty)
                when {
                    responseText.contains("feel the Mayonex gurgling") ->
                        consumeHelper(ConcoctionMayoQueue.MAYONEX, qty, inventory, preferences)
                    responseText.contains("Mayodiol kicks in") ->
                        consumeHelper(ConcoctionMayoQueue.MAYODIOL, qty, inventory, preferences)
                    responseText.contains("Mayostat kicks in") ->
                        consumeHelper(ConcoctionMayoQueue.MAYOSTAT, qty, inventory, preferences)
                    responseText.contains("Mayozapine kicks in") ->
                        consumeHelper(ConcoctionMayoQueue.MAYOZAPINE, qty, inventory, preferences)
                    responseText.contains("Mayoflex kicks in") ->
                        consumeHelper(ConcoctionMayoQueue.MAYOFLEX, qty, inventory, preferences)
                }
            }
            if (responseText.contains("feel the Mayonex gurgling")) {
                MAYONEX_PATTERN.findAll(responseText).forEach { match ->
                    val extra = match.groupValues[1].toIntOrNull() ?: return@forEach
                    preferences?.increment("mayoLevel", extra)
                }
            }
            preferences?.setString("mayoInMouth", "")
            if (adjustFullness && !responseText.contains(" Fullness")) {
                var fullnessUsed = ConsumableDatabase.getFullnessByName(itemName) * qty
                if (responseText.contains("Mayodiol kicks in")) {
                    fullnessUsed = (fullnessUsed - 1).coerceAtLeast(0)
                }
                if (fullnessUsed > 0 && character != null) {
                    val s = character.state.value
                    character.updateConsumables(
                        fullness = s.fullness + fullnessUsed,
                        inebriety = s.inebriety,
                        spleenUsed = s.spleenUsed,
                    )
                }
            }
            decrementPref(preferences, "munchiesPillsUsed", qty)
            decrementPref(preferences, "legendaryNoodlesStomach", qty)
        }

        private fun consumeHelper(
            itemId: Int,
            count: Int,
            inventory: InventoryManager?,
            preferences: Preferences?,
        ) {
            val used = minOf(count, inventory?.getCount(itemId) ?: count)
            if (used > 0) {
                ResultProcessor.processItem(itemId, -used, preferences, inventory = inventory)
            }
        }

        private fun decrementPref(preferences: Preferences?, key: String, amount: Int) {
            val prefs = preferences ?: return
            prefs.setInt(key, (prefs.getInt(key, 0) - amount).coerceAtLeast(0))
        }

        internal fun isEatAbort(responseText: String): Boolean =
            responseText.contains("too full", ignoreCase = true) ||
                responseText.contains("don't feel like eating", ignoreCase = true)

        internal fun eatAbortReason(responseText: String): String = when {
            responseText.contains("too full", ignoreCase = true) -> "Consumption limit reached."
            responseText.contains("don't feel like eating", ignoreCase = true) ->
                "You don't feel like eating."
            else -> "Consumption aborted."
        }
    }
}
