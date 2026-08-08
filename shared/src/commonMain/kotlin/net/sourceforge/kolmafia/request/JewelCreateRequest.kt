package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.CraftMode
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.concoction.CreateItemRequest] JEWELRY — craft.php combine. */
class JewelCreateRequest(
    private val craftRequest: CraftRequest,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val accessibleCount: (Int) -> Int = { 0 },
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)

        if (concoction.ingredients.size != 2) {
            return Result.failure(
                IllegalStateException("JEWEL recipe for '${concoction.result}' is invalid."),
            )
        }

        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                accessibleCount = accessibleCount,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("JEWEL craft not permitted: ${concoction.result}"))
        }

        val ing1 = concoction.ingredients[0]
        val ing2 = concoction.ingredients[1]
        val itemId1 = itemIdFor(ing1)
            ?: return Result.failure(IllegalStateException("Unknown JEWEL ingredient: ${ing1.name}"))
        val itemId2 = itemIdFor(ing2)
            ?: return Result.failure(IllegalStateException("Unknown JEWEL ingredient: ${ing2.name}"))

        var created = 0
        var remaining = quantity
        while (remaining > 0) {
            val batch = remaining
            if (!createItemIngredients.makeIngredients(concoction, batch, state)) {
                return Result.success(created)
            }
            val crafted = craftRequest.craft(CraftMode.COMBINE, batch, itemId1, itemId2)
            if (crafted <= 0) {
                return Result.success(created)
            }
            val gained = crafted.coerceAtMost(remaining)
            created += gained
            remaining -= gained
        }

        return Result.success(created)
    }

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id
}
