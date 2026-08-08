package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.concoction.SushiRequest] — roll and eat at sushi.php. */
class SushiCreateRequest(
    private val client: HttpClient,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
    private val sessionLogger: SessionLogger? = null,
    private val preferences: Preferences? = null,
    private val eventBus: GameEventBus? = null,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)

        val formFields = SushiChoiceMapper.formFields(concoction.result)
            ?: return Result.failure(IllegalStateException("Unknown SUSHI recipe: ${concoction.result}"))

        if (state != null) {
            if (!ConcoctionPermitted.isPermittedMethod(
                    concoction,
                    state,
                    prefs = preferences,
                    limitMode = state.limitMode,
                )
            ) {
                return Result.failure(IllegalStateException("SUSHI craft not permitted: ${concoction.result}"))
            }
            if (!ConsumptionEligibility.canEat(state)) {
                return Result.failure(IllegalStateException("Cannot eat: ${concoction.result}"))
            }
            val fullness = ConsumableDatabase.getFullnessByName(concoction.result)
            if (fullness > 0 &&
                ConsumptionEligibility.effectiveFullnessRemaining(state) < fullness
            ) {
                return Result.failure(IllegalStateException("Not enough fullness for: ${concoction.result}"))
            }
        }

        var consumed = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return Result.success(consumed)
            }

            val response = try {
                client.submitForm(
                    url = "$KOL_BASE_URL/sushi.php",
                    formParameters = toParameters(formFields),
                )
            } catch (e: Exception) {
                return Result.success(consumed)
            }

            if (!response.status.isSuccess()) {
                return Result.success(consumed)
            }
            val body = response.bodyAsText()
            if (!isSuccessResponse(body)) {
                return Result.success(consumed)
            }

            SushiConsumptionSync.registerRequest(formFields, sessionLogger)
            SushiConsumptionSync.parseConsumption(
                formFields = formFields,
                responseText = body,
                updateFullness = state != null,
                character = character,
                eventBus = eventBus,
                preferences = preferences ?: this.preferences,
            )
            inventoryManager?.fetchInventory()

            consumed++
        }

        return Result.success(consumed)
    }

    private fun toParameters(fields: Map<String, String>): Parameters = parameters {
        fields.forEach { (key, value) -> append(key, value) }
    }

    private fun isSuccessResponse(body: String): Boolean =
        !body.contains(TOO_FULL_MESSAGE, ignoreCase = true) &&
            body.contains(SUCCESS_MESSAGE, ignoreCase = true)

    private companion object {
        private const val TOO_FULL_MESSAGE = "too full to eat it"
        private const val SUCCESS_MESSAGE = "You eat the"
    }
}
