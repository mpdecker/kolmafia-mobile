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

/** Desktop FriarRequest — friars.php blessing. */
class FriarRequest(
    private val client: HttpClient,
) {
    suspend fun getBlessing(
        option: Int,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = null,
        knownAscensions: Int = 0,
    ): Result<String> {
        if (option !in 1..3) {
            return Result.failure(IllegalArgumentException("Decide which friar to visit."))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/friars.php",
                formParameters = parameters {
                    append("action", "buffs")
                    append("bro", option.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Friar blessing failed."))
            }
            val html = response.bodyAsText()
            if (html.isEmpty()) {
                return Result.failure(IllegalStateException("You can't find the Deep Fat Friars."))
            }
            parseResponse(html, preferences, questDatabase, knownAscensions)
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BLESSING_RECEIVED_PREF = "friarsBlessingReceived"
        const val LAST_CEREMONY_ASCENSION_PREF = "lastFriarCeremonyAscension"

        val BLESSINGS: List<String> = listOf("food", "familiar", "booze")

        fun findBlessingOption(tag: String): Int {
            val t = tag.trim()
            if (t.isEmpty()) return 0
            if (t[0].isDigit()) {
                val n = t.toIntOrNull() ?: return 0
                return if (n in 1..3) n else 0
            }
            val lower = t.lowercase()
            for ((index, name) in BLESSINGS.withIndex()) {
                if (name.equals(lower, ignoreCase = true)) return index + 1
            }
            return 0
        }

        fun parseResponse(
            html: String,
            preferences: Preferences?,
            questDatabase: QuestDatabase? = null,
            knownAscensions: Int = 0,
        ) {
            if (preferences == null) return
            if (html.contains("one of those per day.") ||
                html.contains("smiles and rubs some ashes")
            ) {
                preferences.setBoolean(BLESSING_RECEIVED_PREF, true)
                preferences.setInt(LAST_CEREMONY_ASCENSION_PREF, knownAscensions)
                questDatabase?.setQuestIfBetter(Quest.FRIAR, QuestDatabase.FINISHED)
            }
        }
    }
}
