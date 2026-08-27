package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LatteChoiceSync
import net.sourceforge.kolmafia.quest.LatteIngredients

/**
 * Desktop [LatteRequest] — refill HTTP + choice-option radio map for CLI refill.
 */
class LatteRequest(
    private val client: HttpClient,
) {
    data class RadioButtons(val l1: String, val l2: String, val l3: String)

    companion object {
        private val LINE_PATTERN = Regex(
            """<tr style=.*?</tr>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val INPUT_PATTERN = Regex(
            """name=["']l(\d)["']\s+(?:checked\s+)?value=["'](.*?)["']>\s*(.*?)\s*</td>""",
            RegexOption.IGNORE_CASE,
        )
        private val REFILL_PATTERN = Regex("""You've got <b>(\d+)</b> refill""")

        fun parseChoiceOptions(
            html: String,
            preferences: Preferences?,
        ): Map<LatteIngredients.Entry, RadioButtons> {
            REFILL_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { remaining ->
                preferences?.setInt("_latteRefillsUsed", 3 - remaining)
            }
            val radio = mutableMapOf<LatteIngredients.Entry, Array<String?>>()
            val unlocks = mutableListOf<String>()
            for (lineMatch in LINE_PATTERN.findAll(html)) {
                val line = lineMatch.value
                for (input in INPUT_PATTERN.findAll(line)) {
                    val button = input.groupValues[1].toIntOrNull() ?: continue
                    val value = input.groupValues[2]
                    val description = input.groupValues[3].trim()
                    for (latte in LatteIngredients.ALL) {
                        val matched = when (button) {
                            1 -> description == latte.first
                            2 -> description == latte.second
                            3 -> description == latte.third
                            else -> false
                        }
                        if (!matched) continue
                        val slots = radio.getOrPut(latte) { arrayOfNulls(3) }
                        slots[button - 1] = value
                        if (button == 1 && !line.contains("&Dagger;") && !line.contains("†")) {
                            unlocks += latte.ingredient
                        }
                        break
                    }
                }
            }
            preferences?.setString("latteUnlocks", unlocks.distinct().joinToString(","))
            return radio.mapValues { (_, slots) ->
                RadioButtons(slots[0].orEmpty(), slots[1].orEmpty(), slots[2].orEmpty())
            }.filterValues { it.l1.isNotBlank() && it.l2.isNotBlank() && it.l3.isNotBlank() }
        }

        fun resolveIngredients(
            first: String,
            second: String,
            third: String,
        ): Array<LatteIngredients.Entry?> {
            fun match(token: String): LatteIngredients.Entry? {
                val t = token.trim().lowercase()
                return LatteIngredients.ALL.firstOrNull {
                    it.ingredient.equals(t, ignoreCase = true) ||
                        it.first.equals(token.trim(), ignoreCase = true) ||
                        it.second.equals(t, ignoreCase = true) ||
                        it.third.equals(token.trim(), ignoreCase = true)
                }
            }
            return arrayOf(match(first), match(second), match(third))
        }
    }

    suspend fun refill(
        first: String,
        second: String,
        third: String,
        preferences: Preferences?,
        sessionLog: (String) -> Unit = {},
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val ingredients = resolveIngredients(first, second, third)
        for (i in 0..2) {
            if (ingredients[i] == null) {
                val name = listOf(first, second, third)[i]
                return Result.failure(
                    IllegalArgumentException(
                        "Cannot find ingredient $name. Use 'latte unlocked' to see available ingredients.",
                    ),
                )
            }
        }
        val unlocks = prefs.getString("latteUnlocks", "")
        for (entry in ingredients) {
            val ingredient = entry!!.ingredient
            if (!unlocks.split(',').map { it.trim() }.any { it.equals(ingredient, ignoreCase = true) }) {
                return Result.failure(
                    IllegalStateException(
                        "Ingredient $ingredient is not unlocked. Use 'latte unlocks' to see how to unlock it.",
                    ),
                )
            }
        }
        return try {
            val visit = client.get("$KOL_BASE_URL/main.php?latte=1").bodyAsText()
            val radios = parseChoiceOptions(visit, prefs)
            val l1 = radios[ingredients[0]]?.l1
            val l2 = radios[ingredients[1]]?.l2
            val l3 = radios[ingredients[2]]?.l3
            if (l1.isNullOrBlank() || l2.isNullOrBlank() || l3.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Could not map latte radio options."))
            }
            val message =
                "Filling mug with ${ingredients[0]!!.first} ${ingredients[1]!!.second} Latte ${ingredients[2]!!.third}."
            sessionLog(message)
            val fillHtml = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = parameters {
                    append("whichchoice", LatteChoiceSync.CHOICE_ID.toString())
                    append("option", "1")
                    append("l1", l1)
                    append("l2", l2)
                    append("l3", l3)
                },
            ).bodyAsText()
            LatteChoiceSync.apply(LatteChoiceSync.CHOICE_ID, 1, fillHtml, prefs, sessionLog = sessionLog)
            Result.success(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
