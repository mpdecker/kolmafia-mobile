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
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop [net.sourceforge.kolmafia.request.concoction.CreateItemRequest] MALUS — guild.php malussmash. */
class MalusCreateRequest(
    private val client: HttpClient,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val skillManager: SkillManager? = null,
    private val sessionLogger: SessionLogger? = null,
    private val eventBus: GameEventBus? = null,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)

        if (concoction.ingredients.size != 1) {
            return Result.failure(
                IllegalStateException("MALUS recipe for '${concoction.result}' is invalid."),
            )
        }

        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                skills = skillManager?.state?.value?.skills ?: emptyList(),
                prefs = preferences,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("MALUS craft not permitted: ${concoction.result}"))
        }

        val ingredient = concoction.ingredients.single()
        val ingredientId = itemIdFor(ingredient)
            ?: return Result.failure(IllegalStateException("Unknown MALUS ingredient: ${ingredient.name}"))

        var created = 0
        repeat(quantity) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return Result.success(created)
            }

            val response = try {
                client.submitForm(
                    url = "$KOL_BASE_URL/guild.php",
                    formParameters = parameters {
                        append("action", "malussmash")
                        append("whichitem", ingredientId.toString())
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
            GuildCreationSync.parseMalus(
                url = "guild.php?action=malussmash&whichitem=$ingredientId&quantity=1",
                responseText = body,
                eventBus = eventBus,
            )
            created++
        }

        return Result.success(created)
    }

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id

    private fun isSuccessResponse(body: String): Boolean =
        body.contains("You acquire", ignoreCase = true)

}
