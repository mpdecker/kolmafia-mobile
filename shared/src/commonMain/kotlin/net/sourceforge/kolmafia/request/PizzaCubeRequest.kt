package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for combining four ingredients in the Diabolic Pizza Cube. */
open class PizzaCubeRequest(
    private val client: HttpClient,
    private val inventoryManager: InventoryManager?,
    private val preferences: Preferences?,
    private val sessionLogger: SessionLogger?,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

    open suspend fun makePizza(ingredients: List<Int>): Result<String> {
        handledSignatures.clear()
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(
                IllegalStateException(
                    RequestAbortGate.lastAbortMessage.ifEmpty {
                        "You are currently in a fight or choice."
                    },
                ),
            )
        }
        val validated = validateIngredients(ingredients, inventoryManager)
            ?: return Result.failure(
                IllegalArgumentException("Pizza Cube requires four owned ingredient item IDs."),
            )
        return try {
            val pizzaField = validated.joinToString(",")
            val response = client.submitForm(
                url = "$KOL_BASE_URL/campground.php",
                formParameters = parameters {
                    append("action", ACTION_PIZZA)
                    append("pizza", pizzaField)
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Pizza Cube request failed."))
            }
            val html = response.bodyAsText()
            val url = "campground.php?action=$ACTION_PIZZA&pizza=$pizzaField"
            if (!parseResponse(url, html)) {
                return Result.failure(IllegalStateException("Pizza Cube response was not successful."))
            }
            sessionLogger?.appendRawLine(sessionLogLine(validated))
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseResponse(url: String, html: String): Boolean {
        val signature = url to html
        if (signature in handledSignatures) return true
        val handled = parseResponse(url, html, inventoryManager, preferences)
        if (!handled) return false
        ResultProcessor.processResults(
            adventureResults = false,
            html = html,
            inventory = inventoryManager,
            preferences = preferences,
        )
        handledSignatures += signature
        return true
    }

    companion object {
        const val DIABOLIC_PIZZA_CUBE_ID = 10335
        const val DIABOLIC_PIZZA_ID = 10336
        const val LAST_PIZZA_PREF = "lastDiabolicPizza"
        const val INGREDIENT_COUNT = 4
        const val ACTION_PIZZA = "pizza"
        const val ACTION_MAKE_PIZZA = "makepizza"

        private val ACTION_FIELD = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
        private val PIZZA_FIELD = Regex("""(?:^|[?&])pizza=(\d+),(\d+),(\d+),(\d+)""")

        fun isPizzaUrl(url: String): Boolean {
            if (!url.contains("campground.php", ignoreCase = true)) return false
            val action = ACTION_FIELD.find(url)?.groupValues?.getOrNull(1)?.lowercase()
            return action == ACTION_PIZZA || action == ACTION_MAKE_PIZZA
        }

        fun parseResponse(
            url: String,
            html: String,
            inventory: InventoryManager?,
            preferences: Preferences?,
        ): Boolean {
            if (!isPizzaUrl(url)) return false
            if (!isSuccessResponse(html)) return false
            val ingredients = urlToIngredients(url) ?: return false
            ResultProcessor.consumeItems(ingredients, preferences, inventory)
            preferences?.setString(LAST_PIZZA_PREF, ingredients.joinToString(","))
            return true
        }

        fun sessionLogLine(ingredients: List<Int>): String {
            val names = ingredients.joinToString(", ") { id ->
                ItemDatabase.getItemName(id).ifEmpty { "item #$id" }
            }
            return "pizza $names"
        }

        internal fun validateIngredients(
            ingredients: List<Int>,
            inventory: InventoryManager?,
        ): List<Int>? {
            if (ingredients.size != INGREDIENT_COUNT) return null
            if (ingredients.any { it <= 0 }) return null
            val inv = inventory ?: return null
            val needed = ingredients.groupingBy { it }.eachCount()
            if (needed.any { (id, qty) -> inv.getCount(id) < qty }) return null
            return ingredients
        }

        internal fun urlToIngredients(url: String): List<Int>? {
            val match = PIZZA_FIELD.find(url) ?: return null
            return listOf(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                match.groupValues[4].toInt(),
            )
        }

        private fun isSuccessResponse(html: String): Boolean =
            html.contains("You acquire", ignoreCase = true) &&
                !html.contains("You don't have that many", ignoreCase = true)
    }
}
