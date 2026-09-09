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

/** Desktop MomRequest — monkeycastle.php mombuff (Phases 6086–6088). */
class MomRequest(
    private val client: HttpClient,
) {
    suspend fun getFood(
        option: Int,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = null,
        inventoryCount: (Int) -> Int = { 0 },
        adventureUnderwater: Boolean = false,
        underwaterFamiliar: Boolean = false,
    ): Result<String> {
        if (option !in 1..7) {
            return Result.failure(IllegalArgumentException("Decide which food to get."))
        }
        if (preferences?.getBoolean(FOOD_RECEIVED_PREF, false) == true) {
            return Result.failure(
                IllegalStateException("You have already had food from Mom Sea Monkee today."),
            )
        }
        accessible(
            questFinished = questDatabase?.isQuestFinished(Quest.SEA_MONKEES) == true,
            inventoryCount = inventoryCount,
            adventureUnderwater = adventureUnderwater,
            underwaterFamiliar = underwaterFamiliar,
        )?.let { reason ->
            return Result.failure(IllegalStateException(reason))
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

        // Desktop MomRequest scuba / mask / familiar underwater item ids.
        const val SCUBA_GEAR = 734
        const val AERATED_DIVING_HELMET = 3607
        const val BATHYSPHERE = 3470
        const val DAS_BOOT = 3609
        const val AMPHIBIOUS_TOPHAT = 4229
        const val SCHOLAR_MASK = 4285
        const val GLADIATOR_MASK = 4284
        const val CRAPPY_MASK = 4282
        const val OLD_SCUBA_TANK = 6315

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

        /**
         * Desktop [MomRequest.accessible] — null when OK, else human-readable block reason.
         */
        fun accessible(
            questFinished: Boolean,
            inventoryCount: (Int) -> Int = { 0 },
            adventureUnderwater: Boolean = false,
            underwaterFamiliar: Boolean = false,
        ): String? {
            if (!questFinished) return "You haven't rescued Mom yet."
            val hasSelfGear = adventureUnderwater ||
                listOf(
                    AERATED_DIVING_HELMET,
                    SCHOLAR_MASK,
                    GLADIATOR_MASK,
                    CRAPPY_MASK,
                    SCUBA_GEAR,
                    OLD_SCUBA_TANK,
                ).any { inventoryCount(it) > 0 }
            if (!hasSelfGear) {
                return "You don't have the right equipment to adventure underwater."
            }
            val hasFamiliarGear = underwaterFamiliar ||
                listOf(AMPHIBIOUS_TOPHAT, DAS_BOOT, BATHYSPHERE).any { inventoryCount(it) > 0 }
            if (!hasFamiliarGear) {
                return "Your familiar doesn't have the right equipment to adventure underwater."
            }
            return null
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
