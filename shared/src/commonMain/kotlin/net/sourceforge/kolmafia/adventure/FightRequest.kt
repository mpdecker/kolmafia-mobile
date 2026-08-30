package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

class FightRequest(private val client: HttpClient) {
    suspend fun fight(
        macroText: String,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<String> = execute(FightAction.macro(macroText), sessionLogger, preferences)

    suspend fun attack(
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<String> = execute(FightAction.attack(), sessionLogger, preferences)

    suspend fun skill(
        skillId: Int,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<String> = execute(FightAction.skill(skillId), sessionLogger, preferences)

    suspend fun item(
        itemId: Int,
        secondItemId: Int = 0,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<String> = execute(FightAction.item(itemId, secondItemId), sessionLogger, preferences)

    suspend fun runaway(
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<String> = execute(FightAction.runaway(), sessionLogger, preferences)

    suspend fun execute(
        action: FightAction,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<String> {
        val fields = action.formFields()
        RequestLogger.registerRequest(
            "fight.php",
            sessionLogger = sessionLogger,
            preferences = preferences,
            formFields = fields,
        )
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/fight.php",
                formParameters = Parameters.build {
                    fields.forEach { (name, value) -> append(name, value) }
                },
            )
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes a typed action and follows a multi-fight response until KoL
     * returns a non-fight page or [maxRounds] is reached.
     */
    suspend fun executeMultiFight(
        action: FightAction,
        maxRounds: Int = 100,
        sessionLogger: SessionLogger? = null,
        preferences: Preferences? = null,
    ): Result<List<String>> {
        if (maxRounds <= 0) return Result.success(emptyList())
        val responses = mutableListOf<String>()
        var next = action
        repeat(maxRounds) {
            val response = execute(next, sessionLogger, preferences).getOrElse {
                return Result.failure(it)
            }
            responses += response
            if (!AdventureParser.isInMultiFight(response)) return Result.success(responses)
            // Repeating a typed action is the closest equivalent to the
            // desktop multi-fight loop when no combat script is supplied.
            next = action
            return@repeat
        }
        return Result.success(responses)
    }
}
