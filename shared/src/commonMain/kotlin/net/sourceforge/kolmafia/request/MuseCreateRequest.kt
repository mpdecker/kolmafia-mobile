package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [MultiUseRequest] — multi-use first ingredient via inv_use.php or multiuse.php. */
class MuseCreateRequest(
    private val useItemRequest: UseItemRequest,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val inventoryManager: InventoryManager? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

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

        val prefs = preferences ?: this.preferences
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = prefs,
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
            handledSignatures.clear()
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
            val html = response.getOrThrow()
            val url = responseUrl(firstItemId, firstIngredient.quantity)
            if (!parseResponse(url, html, concoction)) {
                return Result.success(created)
            }
            sessionLogger?.appendRawLine(sessionLogLine(concoction))
            created++
        }

        return Result.success(created)
    }

    fun parseResponse(url: String, html: String, concoction: ConcoctionData): Boolean {
        val signature = url to html
        if (signature in handledSignatures) return true
        if (!isSuccessResponse(html)) return false
        val ingredientIds = concoction.ingredients.map { ingredient ->
            val itemId = itemIdFor(ingredient) ?: return false
            List(ingredient.quantity.coerceAtLeast(1)) { itemId }
        }.flatten()
        ResultProcessor.consumeItems(ingredientIds, preferences, inventoryManager)
        ResultProcessor.processResults(
            adventureResults = false,
            html = html,
            inventory = inventoryManager,
            preferences = preferences,
        )
        handledSignatures += signature
        return true
    }

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id

    private fun isSuccessResponse(body: String): Boolean =
        body.contains("You acquire", ignoreCase = true) &&
            !body.contains("You don't have that many", ignoreCase = true)

    private fun responseUrl(itemId: Int, quantity: Int): String =
        if (quantity == 1) {
            "inv_use.php?which=3&whichitem=$itemId&ajax=1"
        } else {
            "multiuse.php?action=useitem&whichitem=$itemId&quantity=$quantity"
        }

    private fun sessionLogLine(concoction: ConcoctionData): String = buildString {
        append("Use ")
        concoction.ingredients.forEachIndexed { index, ingredient ->
            if (index > 0) append(" + ")
            append(ingredient.quantity)
            append(' ')
            append(ingredient.name)
        }
    }
}
