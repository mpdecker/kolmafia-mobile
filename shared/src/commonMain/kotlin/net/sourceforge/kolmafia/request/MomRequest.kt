package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Desktop MomRequest — monkeycastle.php mombuff (no scuba checkpoint this phase). */
class MomRequest(
    private val client: HttpClient,
) {
    suspend fun getFood(
        option: Int,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = null,
    ): Result<String> {
        if (option !in 1..7) {
            return Result.failure(IllegalArgumentException("Decide which food to get."))
        }
        if (preferences?.getBoolean(FOOD_RECEIVED_PREF, false) == true) {
            return Result.failure(
                IllegalStateException("You have already had food from Mom Sea Monkee today."),
            )
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/monkeycastle.php",
                formParameters = parameters {
                    append("action", "mombuff")
                    append("whichbuff", option.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Mom request failed."))
            }
            val html = response.bodyAsText()
            if (html.isEmpty() ||
                html.contains("visit the Sea Monkees without some way of breathing underwater")
            ) {
                return Result.failure(IllegalStateException("You can't get to Mom Sea Monkee"))
            }
            parseResponse(html, preferences, questDatabase)
            if (!html.contains("You acquire an effect") && preferences != null) {
                // Already used today — still mark received (desktop processResults).
                preferences.setBoolean(FOOD_RECEIVED_PREF, true)
            }
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val FOOD_RECEIVED_PREF = "_momFoodReceived"

        val FOOD: List<String> = listOf(
            "hot", "cold", "stench", "spooky", "sleaze", "critical", "stats",
        )

        fun findFoodOption(tag: String): Int {
            val t = tag.trim()
            if (t.isEmpty()) return 0
            if (t[0].isDigit()) {
                val n = t.toIntOrNull() ?: return 0
                return if (n in 1..7) n else 0
            }
            val lower = t.lowercase()
            for ((index, name) in FOOD.withIndex()) {
                if (name.equals(lower, ignoreCase = true)) return index + 1
            }
            return 0
        }

        fun parseResponse(
            html: String,
            preferences: Preferences?,
            questDatabase: QuestDatabase? = null,
        ) {
            if (preferences == null) return
            val success = html.contains("begin to sweat") ||
                html.contains("break out in a cold sweat") ||
                html.contains("feel gross") ||
                html.contains("feel... wrong") ||
                html.contains("begin to sweat with anxiety") ||
                html.contains("blood spreads out around") ||
                html.contains("heard it before")
            if (success) {
                preferences.setBoolean(FOOD_RECEIVED_PREF, true)
                questDatabase?.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.FINISHED)
            }
        }
    }
}
