package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop WaxGlob / BurningNewspaper / Meteoroid / GrubbyWool create pattern:
 * inv_use source → choice.php option loop.
 */
class ChoiceUseCreateRequest(
    private val useItemRequest: UseItemRequest,
    private val choiceRequest: ChoiceRequest,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val sourceItemId: Int,
    private val choiceId: Int,
    private val itemIdToOption: (Int) -> Int,
    private val exitOption: Int = 0,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (CreateAbortGate.shouldAbort()) return Result.success(0)
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(concoction, state, prefs = preferences, limitMode = state.limitMode)
        ) {
            return Result.failure(IllegalStateException("Create not permitted: ${concoction.result}"))
        }
        val resultId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown result: ${concoction.result}"))
        val option = itemIdToOption(resultId)
        if (option <= 0) {
            return Result.failure(IllegalStateException("No choice option for ${concoction.result}"))
        }

        var created = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return Result.success(created)
            }
            if (useItemRequest.use(sourceItemId, 1).isFailure) {
                return Result.success(created)
            }
            val body = choiceRequest.choose(choiceId, option).getOrElse {
                return Result.success(created)
            }.first
            if (!body.contains("You acquire", ignoreCase = true)) {
                return Result.success(created)
            }
            created++
        }
        if (exitOption > 0) {
            choiceRequest.choose(choiceId, exitOption)
        }
        return Result.success(created)
    }

    companion object {
        const val WAX_GLOB = 9310
        const val WAX_CHOICE = 1218
        const val BURNING_NEWSPAPER = 9683
        const val NEWSPAPER_CHOICE = 1277
        const val METAL_METEOROID = 9516
        const val METEOROID_CHOICE = 1264
        const val GRUBBY_WOOL = 11091
        const val WOOL_CHOICE = 1490

        fun waxOption(itemId: Int): Int = when (itemId) {
            9306 -> 1 // miniature candle
            9305 -> 2 // wax hand
            9308 -> 3 // wax face
            9307 -> 4 // wax pancake
            9309 -> 5 // wax booze
            else -> 0
        }

        fun newspaperOption(itemId: Int): Int = when (itemId) {
            9684 -> 1 // burning paper hat
            9685 -> 2 // burning cape
            9686 -> 3 // burning paper slippers
            9687 -> 4 // burning paper jorts
            9688 -> 5 // burning paper crane
            else -> 0
        }

        fun meteoroidOption(itemId: Int): Int = when (itemId) {
            9517 -> 1
            9518 -> 2
            9519 -> 3
            9520 -> 4
            9521 -> 5
            9522 -> 6
            else -> 0
        }

        fun woolOption(itemId: Int): Int = when (itemId) {
            11092 -> 1
            11093 -> 2
            11094 -> 3
            11095 -> 4
            11096 -> 5
            11098 -> 6 // grubby woolball
            else -> 0
        }
    }
}
