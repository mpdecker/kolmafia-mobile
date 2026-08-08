package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.concoction.StillSync
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.stillShopRow
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.ShopRequest

/** Desktop [StillRequest] — cocktail still shop buy after ingredient retrieve. */
class StillCreateRequest(
    private val shopRequest: ShopRequest?,
    private val createItemIngredients: CreateItemIngredients?,
    private val gameDatabase: GameDatabase?,
    private val character: KoLCharacter?,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val row = concoction.stillShopRow()
            ?: return Result.failure(IllegalStateException("Missing still shop row for: ${concoction.result}"))
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("Still craft not permitted: ${concoction.result}"))
        }

        val helper = createItemIngredients
            ?: return Result.failure(IllegalStateException("CreateItemIngredients not configured"))
        val shop = shopRequest
            ?: return Result.failure(IllegalStateException("ShopRequest not configured"))

        if (!helper.makeIngredients(concoction, quantity, state)) {
            return Result.failure(
                IllegalStateException("Could not retrieve ingredients for ${concoction.result}"),
            )
        }

        val response = shop.buy(STILL_SHOP_ID, row, quantity)
        response.exceptionOrNull()?.let { return Result.failure(it) }
        val body = response.getOrThrow()
        if (!body.contains("You acquire")) {
            return Result.failure(IllegalStateException("Still upgrading was unsuccessful."))
        }
        StillSync.parseStillsAvailable(body)?.let { character?.setStillsAvailable(it) }
        return Result.success(quantity)
    }

    companion object {
        const val STILL_SHOP_ID = "still"
    }
}
