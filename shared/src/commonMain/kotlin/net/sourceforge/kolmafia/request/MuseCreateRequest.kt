package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [MultiUseRequest] — multi-use first ingredient via inv_use.php or multiuse.php. */
class MuseCreateRequest(
    private val useItemRequest: UseItemRequest,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)

        if (concoction.ingredients.isEmpty()) {
            return Result.failure(IllegalStateException("MUSE recipe for '${concoction.result}' is invalid."))
        }

        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("MUSE craft not permitted: ${concoction.result}"))
        }

        val firstIngredient = concoction.ingredients.first()
        val firstItemId = itemIdFor(firstIngredient)
            ?: return Result.failure(IllegalStateException("Unknown MUSE ingredient: ${firstIngredient.name}"))

        var created = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return Result.success(created)
            }

            val response = if (firstIngredient.quantity == 1) {
                useItemRequest.use(firstItemId, 1)
            } else {
                useItemRequest.multiUse(firstItemId, firstIngredient.quantity)
            }
            if (response.isFailure) {
                return Result.success(created)
            }
            if (!isSuccessResponse(response.getOrThrow())) {
                return Result.success(created)
            }
            created++
        }

        return Result.success(created)
    }

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id

    private fun isSuccessResponse(body: String): Boolean =
        body.contains("You acquire", ignoreCase = true) &&
            !body.contains("You don't have that many", ignoreCase = true)
}
