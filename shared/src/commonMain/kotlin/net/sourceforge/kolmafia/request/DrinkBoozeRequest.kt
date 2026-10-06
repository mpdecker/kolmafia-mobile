package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ConsumptionHelperState
import net.sourceforge.kolmafia.session.SessionLogger

class DrinkBoozeRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val sessionLogger: SessionLogger? = null,
    private val retrieveItem: (suspend (Int, Int) -> Int)? = null,
    private val equipmentManager: net.sourceforge.kolmafia.session.EquipmentManager? = null,
    private val effectManager: net.sourceforge.kolmafia.effect.EffectManager? = null,
) {
    suspend fun drink(itemId: Int, quantity: Int = 1): Result<String> =
        consumeDrink(itemId, quantity).fold(
            onSuccess = { outcome ->
                when (outcome) {
                    is ConsumptionRequestOutcome.Completed -> Result.success("")
                    is ConsumptionRequestOutcome.Aborted ->
                        Result.failure(IllegalStateException(outcome.reason))
                }
            },
            onFailure = { Result.failure(it) },
        )

    suspend fun consumeDrink(itemId: Int, quantity: Int = 1): Result<ConsumptionRequestOutcome> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        if (quantity <= 0) {
            return Result.success(ConsumptionRequestOutcome.Completed(0))
        }

        if (itemId == ICE_STEIN) {
            val need = quantity
            val got = retrieveItem?.invoke(ICE_COLD_SIX_PACK, need) ?: 0
            val have = inventoryManager?.getCount(ICE_COLD_SIX_PACK) ?: got
            if (have < need && retrieveItem != null) {
                return Result.success(
                    ConsumptionRequestOutcome.Aborted(
                        0,
                        "Insufficient ice-cold-six-packs available.",
                    ),
                )
            }
        }

        val autoAbort = ConsumeAutomation.prepareDrink(
            itemId = itemId,
            count = quantity,
            preferences = preferences,
            character = character,
            inventory = inventoryManager,
            equipmentManager = equipmentManager,
            effectManager = effectManager,
        )
        if (autoAbort != null) {
            return Result.success(ConsumptionRequestOutcome.Aborted(0, autoAbort))
        }

        val iterations = iterationCount(itemId, quantity)
        var totalConsumed = 0

        for (iteration in 1..iterations) {
            ConsumptionHelperState.beginIteration(QueueBucket.BOOZE, iteration)
            val iterQty = if (iterations > 1) 1 else quantity
            val utensil = ConsumptionHelperState.utensilForDrink()

            if (utensil != null) {
                val elementalAbort = ElementalHelper.prepareForUtensil(utensil)
                if (elementalAbort.isNotEmpty()) {
                    return Result.success(
                        ConsumptionRequestOutcome.Aborted(totalConsumed, elementalAbort),
                    )
                }
            }

            val httpResult = performDrink(itemId, iterQty, utensil)
            httpResult.exceptionOrNull()?.let { return Result.failure(it) }

            val body = httpResult.getOrThrow()
            UseItemConsumptionSync.rememberLastItem(itemId, iterQty)
            if (isDrinkAbort(body)) {
                UseItemConsumptionSync.clearLastItem()
                return Result.success(
                    ConsumptionRequestOutcome.Aborted(totalConsumed, drinkAbortReason(body)),
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
                        UseItemConsumptionSync.lastUpdate.ifBlank { drinkAbortReason(body) },
                    ),
                )
            }

            totalConsumed += iterQty
            if (utensil != null) {
                ConsumptionHelperState.decrementDrinkHelper()
            }
        }

        ConsumptionHelperState.markFullyConsumed(QueueBucket.BOOZE, totalConsumed)
        return Result.success(ConsumptionRequestOutcome.Completed(totalConsumed))
    }

    fun queueDrinkHelper(itemId: Int, quantity: Int): Result<Unit> {
        ConsumptionHelperState.queueDrinkHelper(itemId, quantity)
        return Result.success(Unit)
    }

    private suspend fun performDrink(itemId: Int, quantity: Int, utensilId: Int?): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/inv_booze.php") {
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
        // Desktop sequentialConsume: TPS drinks drink one-at-a-time when inventory is short
        if (sequentialConsume(itemId)) {
            val have = inventoryManager?.getCount(itemId) ?: 0
            if (have < quantity) return quantity
        }
        return 1
    }

    private fun singleConsume(itemId: Int): Boolean {
        if (ConsumptionHelperState.currentDrinkHelper() != null) return true
        return itemId == ICE_STEIN
    }

    companion object {
        private const val ICE_STEIN = 1618
        private const val ICE_COLD_SIX_PACK = 138
        const val DIRTY_MARTINI = 948
        const val GROGTINI = 949
        const val CHERRY_BOMB = 950
        const val VESPER = 1023
        const val BODYSLAM = 1024
        const val SANGRIA_DEL_DIABLO = 1025

        /** Desktop [DrinkItemRequest.sequentialConsume] — tiny plastic sword drinks. */
        fun sequentialConsume(itemId: Int): Boolean = when (itemId) {
            DIRTY_MARTINI, GROGTINI, CHERRY_BOMB, VESPER, BODYSLAM, SANGRIA_DEL_DIABLO -> true
            else -> false
        }

        /** Desktop [DrinkItemRequest] mime shotglass / flagellate flagon consume side effects. */
        fun parseDrinkHelpers(responseText: String, preferences: Preferences?) {
            val prefs = preferences ?: return
            if (responseText.contains("You pour your drink into your mime army shotglass")) {
                prefs.setBoolean("_mimeArmyShotglassUsed", true)
            }
            if (responseText.contains("You pour your drink into your flagellate flagon.")) {
                prefs.setInt(
                    "flagellateFlagonsActive",
                    (prefs.getInt("flagellateFlagonsActive", 0) - 1).coerceAtLeast(0),
                )
            }
        }

        internal fun isDrinkAbort(responseText: String): Boolean =
            responseText.contains("too drunk", ignoreCase = true) ||
                responseText.contains("don't feel like drinking", ignoreCase = true)

        internal fun drinkAbortReason(responseText: String): String = when {
            responseText.contains("too drunk", ignoreCase = true) -> "Inebriety limit reached."
            responseText.contains("don't feel like drinking", ignoreCase = true) ->
                "You don't feel like drinking."
            else -> "Consumption aborted."
        }
    }
}
