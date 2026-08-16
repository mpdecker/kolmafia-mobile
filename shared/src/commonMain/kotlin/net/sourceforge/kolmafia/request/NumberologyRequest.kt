package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.NumberologyManager

/** Desktop [NumberologyRequest] — Calculate the Universe skill cast + choice 1103 seed submit. */
open class NumberologyRequest(private val client: HttpClient) {

    open suspend fun calculate(
        seed: Int,
        preferences: Preferences?,
        characterState: CharacterState,
    ): Result<String> {
        val absSeed = kotlin.math.abs(seed)
        val skillLevel = preferences?.getInt(PREF_SKILL_LEVEL, 0) ?: 0
        val used = preferences?.getInt(PREF_UNIVERSE_CALCULATED, 0) ?: 0
        if (skillLevel <= used) {
            return Result.failure(IllegalStateException("You already Calculated the Universe today."))
        }
        if (characterState.adventuresLeft <= 0) {
            return Result.failure(
                IllegalStateException("You don't have time to Calculate the Universe right now."),
            )
        }
        val result = NumberologyManager.numberology(characterState, absSeed)
        val prize = NumberologyManager.numberologyPrize(result)
        if (prize == NumberologyManager.TRY_AGAIN) {
            return Result.failure(
                IllegalStateException("Seed $absSeed will result in Try Again."),
            )
        }
        return try {
            val skillResponse = client.get("$KOL_BASE_URL/runskillz.php") {
                parameter("action", "Skillz")
                parameter("whichskill", SKILL_ID.toString())
                parameter("ajax", "1")
            }
            if (!skillResponse.status.isSuccess()) {
                return Result.failure(IllegalStateException("You can't Calculate the Universe"))
            }
            val skillText = skillResponse.bodyAsText()
            when {
                skillText.contains("You don't have that skill") ->
                    return Result.failure(
                        IllegalStateException("You don't know how to Calculate the Universe"),
                    )
                skillText.contains("You don't have enough") ->
                    return Result.failure(
                        IllegalStateException("You need at least 1 MP to Calculate the Universe"),
                    )
                skillText.contains("You can't use that skill again today") -> {
                    preferences?.setInt(PREF_UNIVERSE_CALCULATED, skillLevel)
                    return Result.failure(
                        IllegalStateException("You already Calculated the Universe today"),
                    )
                }
                !skillText.contains("whichchoice") ->
                    return Result.failure(IllegalStateException("You can't Calculate the Universe"))
            }
            val choiceResponse = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = parameters {
                    append("whichchoice", CHOICE_ID.toString())
                    append("option", "1")
                    append("num", absSeed.toString())
                },
            )
            if (choiceResponse.status.isSuccess()) {
                Result.success(choiceResponse.bodyAsText())
            } else {
                Result.failure(IllegalStateException("You can't Calculate the Universe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SKILL_ID = 144
        const val CHOICE_ID = 1103
        const val PREF_SKILL_LEVEL = "skillLevel144"
        const val PREF_UNIVERSE_CALCULATED = "_universeCalculated"
    }
}
