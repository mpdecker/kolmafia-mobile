package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.RetrieveItemService

/** Desktop CreateItemRequest.makeDough — rolling/unrolling pin dough conversion. */
class RollingPinCreateRequest(
    private val useItemRequest: UseItemRequest?,
    private val retrieveItemService: RetrieveItemService?,
    private val gameDatabase: GameDatabase?,
) {
    suspend fun create(concoction: ConcoctionData, quantity: Int): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val outputId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown roll output: ${concoction.result}"))
        val recipe = DOUGH_RECIPES.firstOrNull { it.outputId == outputId }
            ?: return Result.failure(IllegalStateException("Unsupported roll output: ${concoction.result}"))
        val retrieve = retrieveItemService
            ?: return Result.failure(IllegalStateException("RetrieveItemService not configured"))
        val use = useItemRequest
            ?: return Result.failure(IllegalStateException("UseItemRequest not configured"))

        var created = 0
        repeat(quantity) {
            val inputNeeded = recipe.inputQuantity
            if (retrieve.retrieve(recipe.inputId, inputNeeded) < inputNeeded) {
                return Result.failure(
                    IllegalStateException("Could not retrieve $inputNeeded of ${recipe.inputName}"),
                )
            }
            val usedPin = retrieve.retrieve(recipe.toolId, 1) >= 1
            val response = if (usedPin) {
                use.use(recipe.toolId, 1)
            } else {
                use.use(recipe.inputId, 1)
            }
            response.exceptionOrNull()?.let { return Result.failure(it) }
            val body = response.getOrThrow()
            if (!body.contains("You acquire")) {
                return Result.failure(IllegalStateException("Rolling pin creation was unsuccessful."))
            }
            created++
        }
        return Result.success(created)
    }

    private data class DoughRecipe(
        val outputId: Int,
        val inputId: Int,
        val inputName: String,
        val inputQuantity: Int,
        val toolId: Int,
    )

    companion object {
        private const val WAD_OF_DOUGH = 159
        private const val FLAT_DOUGH = 301
        private const val ROLLING_PIN = 873
        private const val UNROLLING_PIN = 874

        private val DOUGH_RECIPES = listOf(
            DoughRecipe(
                outputId = FLAT_DOUGH,
                inputId = WAD_OF_DOUGH,
                inputName = "wad of dough",
                inputQuantity = 1,
                toolId = ROLLING_PIN,
            ),
            DoughRecipe(
                outputId = WAD_OF_DOUGH,
                inputId = FLAT_DOUGH,
                inputName = "flat dough",
                inputQuantity = 1,
                toolId = UNROLLING_PIN,
            ),
        )
    }
}
