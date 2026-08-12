package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.ConsumptionHelperState

open class EatFoodRequest(private val client: HttpClient) {
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
            if (isEatAbort(body)) {
                return Result.success(
                    ConsumptionRequestOutcome.Aborted(totalConsumed, eatAbortReason(body)),
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

    internal companion object {
        private const val BLACK_PUDDING = 2338
        private const val SMORE = 5071

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
