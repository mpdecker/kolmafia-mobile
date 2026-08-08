package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.ConsumptionHelperState

class DrinkBoozeRequest(private val client: HttpClient) {
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
        if (quantity <= 0) {
            return Result.success(ConsumptionRequestOutcome.Completed(0))
        }

        val iterations = iterationCount(itemId, quantity)
        var totalConsumed = 0

        for (iteration in 1..iterations) {
            ConsumptionHelperState.beginIteration(QueueBucket.BOOZE, iteration)
            val iterQty = if (iterations > 1) 1 else quantity
            val utensil = ConsumptionHelperState.utensilForDrink()

            val httpResult = performDrink(itemId, iterQty, utensil)
            httpResult.exceptionOrNull()?.let { return Result.failure(it) }

            val body = httpResult.getOrThrow()
            if (isDrinkAbort(body)) {
                return Result.success(
                    ConsumptionRequestOutcome.Aborted(totalConsumed, drinkAbortReason(body)),
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
        return 1
    }

    private fun singleConsume(itemId: Int): Boolean {
        if (ConsumptionHelperState.currentDrinkHelper() != null) return true
        return itemId == ICE_STEIN
    }

    internal companion object {
        private const val ICE_STEIN = 1618

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
