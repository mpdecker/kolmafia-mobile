package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.vykea.VykeaCompanionManager

/** Desktop [VYKEARequest] — VYKEA instructions + choice 1120–1123 assembly chain. */
class VykeaCreateRequest(
    private val useItemRequest: UseItemRequest,
    private val choiceRequest: ChoiceRequest,
    private val retrieveItemService: RetrieveItemService,
    private val createItemIngredients: CreateItemIngredients,
    private val vykeaCompanionManager: VykeaCompanionManager,
    private val gameDatabase: GameDatabase?,
    private val accessibleCount: (Int) -> Int = { 0 },
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        val cappedQuantity = quantity.coerceAtMost(1)
        if (cappedQuantity <= 0) return Result.success(0)

        if (vykeaCompanionManager.hasStoredCompanion()) {
            return Result.failure(
                IllegalStateException(
                    "You already have a VYKEA companion. It would get jealous and turn on you if you build another one today.",
                ),
            )
        }

        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
                accessibleCount = accessibleCount,
            )
        ) {
            return Result.failure(IllegalStateException("VYKEA craft not permitted: ${concoction.result}"))
        }

        if (concoction.ingredients.size < 3 ||
            !concoction.ingredients[0].name.equals("VYKEA instructions", ignoreCase = true)
        ) {
            return Result.failure(
                IllegalStateException("VYKEA companion recipe for '${concoction.result}' is invalid."),
            )
        }

        if (retrieveItemService.retrieve(VykeaChoiceMapper.HEX_KEY_ID, 1) < 1) {
            return Result.failure(
                IllegalStateException("You need a VYKEA hex key in order to build a VYKEA companion."),
            )
        }

        if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
            return Result.failure(
                IllegalStateException("Could not retrieve ingredients for ${concoction.result}"),
            )
        }

        if (retrieveItemService.retrieve(VykeaChoiceMapper.PLANK_ID, STARTER_COMPONENT_COUNT) < STARTER_COMPONENT_COUNT ||
            retrieveItemService.retrieve(VykeaChoiceMapper.RAIL_ID, STARTER_COMPONENT_COUNT) < STARTER_COMPONENT_COUNT ||
            retrieveItemService.retrieve(VykeaChoiceMapper.BRACKET_ID, STARTER_COMPONENT_COUNT) < STARTER_COMPONENT_COUNT
        ) {
            return Result.failure(
                IllegalStateException(
                    "You need a 5 planks, 5 rails, and 5 brackets in order to start construction.",
                ),
            )
        }

        val useResponse = useItemRequest.use(VykeaChoiceMapper.INSTRUCTIONS_ID, 1)
        if (useResponse.isFailure) {
            return Result.failure(IllegalStateException("VYKEA companion creation failed."))
        }

        var choiceId = ChoiceUtilities.extractChoiceId(useResponse.getOrThrow()) ?: 0
        if (choiceId == 0) {
            return Result.failure(IllegalStateException("VYKEA companion creation failed."))
        }

        var index = 1
        while (index < concoction.ingredients.size) {
            val ingredient = concoction.ingredients[index]
            val itemId = itemIdFor(ingredient)
                ?: return Result.failure(IllegalStateException("VYKEA companion recipe is incorrect."))
            val option = VykeaChoiceMapper.optionFor(choiceId, itemId, ingredient.quantity)
            if (option == 0) {
                return Result.failure(IllegalStateException("VYKEA companion recipe is incorrect."))
            }

            val choiceResult = choiceRequest.choose(choiceId, option)
            if (choiceResult.isFailure) {
                return Result.failure(IllegalStateException("VYKEA companion creation failed."))
            }
            val (body, url) = choiceResult.getOrThrow()
            choiceId = ChoiceUtilities.extractChoiceId(body)
                ?: ChoiceUtilities.extractChoiceFromUrl(url).takeIf { it > 0 }
                ?: 0

            if (VykeaChoiceMapper.consumesIngredient(option)) {
                index++
            } else if (choiceId == 0) {
                return Result.failure(IllegalStateException("VYKEA companion creation failed."))
            }
        }

        return Result.success(cappedQuantity)
    }

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id

    companion object {
        private const val STARTER_COMPONENT_COUNT = 5
    }
}
