package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.concoction.PhineasRequest] — sealhide trade at volcanoisland.php. */
class PhineasCreateRequest(
    private val client: HttpClient,
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
            return Result.failure(
                IllegalStateException("PHINEAS recipe for '${concoction.result}' is invalid."),
            )
        }

        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("PHINEAS craft not permitted: ${concoction.result}"))
        }

        val resultItemId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown PHINEAS result: ${concoction.result}"))

        var created = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return Result.success(created)
            }

            val response = try {
                client.submitForm(
                    url = "$KOL_BASE_URL/volcanoisland.php",
                    formParameters = parameters {
                        append("action", "npc")
                        append("subaction", "make")
                        append("makewhich", resultItemId.toString())
                        append("quantity", "1")
                    },
                )
            } catch (e: Exception) {
                return Result.success(created)
            }

            if (!response.status.isSuccess()) {
                return Result.success(created)
            }
            val body = response.bodyAsText()
            if (!isSuccessResponse(body)) {
                return Result.success(created)
            }
            created++
        }

        return Result.success(created)
    }

    private fun isSuccessResponse(body: String): Boolean =
        body.contains("You acquire", ignoreCase = true)
}
