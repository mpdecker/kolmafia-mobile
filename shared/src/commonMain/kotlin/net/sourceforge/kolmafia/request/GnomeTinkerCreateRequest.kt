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

/** Desktop [net.sourceforge.kolmafia.request.concoction.GnomeTinkerRequest] — supertinkering at gnomes.php. */
class GnomeTinkerCreateRequest(
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

        if (concoction.ingredients.size != 3) {
            return Result.failure(
                IllegalStateException("GNOME_TINKER recipe for '${concoction.result}' requires 3 ingredients."),
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
            return Result.failure(IllegalStateException("GNOME_TINKER craft not permitted: ${concoction.result}"))
        }

        val itemIds = concoction.ingredients.map { ingredient ->
            itemIdFor(ingredient)
                ?: return Result.failure(IllegalStateException("Unknown GNOME_TINKER ingredient: ${ingredient.name}"))
        }

        var created = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return Result.success(created)
            }

            val response = try {
                client.submitForm(
                    url = "$KOL_BASE_URL/gnomes.php",
                    formParameters = parameters {
                        append("place", "tinker")
                        append("action", "tinksomething")
                        append("item1", itemIds[0].toString())
                        append("item2", itemIds[1].toString())
                        append("item3", itemIds[2].toString())
                        append("qty", "1")
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

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id

    private fun isSuccessResponse(body: String): Boolean =
        body.contains(SUCCESS_MESSAGE, ignoreCase = true)

    private companion object {
        private const val SUCCESS_MESSAGE = "Gnorman deftly assembles your items"
    }
}
