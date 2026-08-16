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

/** Desktop [GrandpaRequest] — Sea Grandpa story query via monkeycastle.php. */
open class GrandpaRequest(private val client: HttpClient) {

    open suspend fun ask(
        topic: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
    ): Result<String> {
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/monkeycastle.php",
                formParameters = parameters {
                    append("action", "grandpastory")
                    append("topic", topic)
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("HTTP ${response.status.value}"))
            }
            val html = response.bodyAsText()
            if (html.contains("can't visit the Sea Monkees")) {
                return Result.failure(
                    IllegalStateException("You're not equipped to visit the Sea Monkees."),
                )
            }
            parseResponse(topic, html, preferences, questDatabase)
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val STORY_FLAG_PREFS = mapOf(
            "avius ticklium" to "grandpaUnlockedFishyWand",
            "hierfal" to "hasTwinkleVision",
            "eel" to "grandpaUnlockedEelSauce",
            "neptune flytrap" to "grandpaUnlockedWaterPoloCap",
            "neptune" to "grandpaUnlockedWaterPoloCap",
            "flytrap" to "grandpaUnlockedWaterPoloCap",
            "octopus" to "grandpaUnlockedSeaRadish",
            "diver" to "grandpaUnlockedGlowingSyringe",
            "reef" to "grandpaUnlockedJellyfishGel",
            "belle" to "grandpaUnlockedWaterPoloMitt",
            "fisherfish" to "grandpaUnlockedHalibut",
            "mine" to "grandpaUnlockedMarineAquamarine",
            "anemone" to "grandpaUnlockedMarineAquamarine",
            "clownfish" to "grandpaUnlockedMidgetClownfish",
            "lounge lizardfish" to "grandpaUnlockedHairOfTheFish",
            "lizardfish" to "grandpaUnlockedHairOfTheFish",
            "nurse shark" to "grandpaUnlockedBlankPrescriptionSheet",
            "nurse" to "grandpaUnlockedBlankPrescriptionSheet",
            "scales" to "grandpaUnlockedHeavilyInvestedInPunFutures",
            "scale" to "grandpaUnlockedHeavilyInvestedInPunFutures",
            "trophy fish" to "grandpaUnlockedTrophyFish",
            "trophyfish" to "grandpaUnlockedTrophyFish",
            "groupie" to "grandpaUnlockedGroupieSpangles",
            "currents" to "intenseCurrents",
        )

        fun parseResponse(
            topic: String,
            @Suppress("UNUSED_PARAMETER") responseText: String,
            preferences: Preferences?,
            questDatabase: QuestDatabase?,
        ) {
            val normalized = topic.lowercase().trim()
            when (normalized) {
                "grandma", "wife" -> questDatabase?.setQuestIfBetter(Quest.SEA_MONKEES, "step6")
                else -> {
                    val pref = STORY_FLAG_PREFS[normalized] ?: return
                    preferences?.setBoolean(pref, true)
                }
            }
        }
    }
}
