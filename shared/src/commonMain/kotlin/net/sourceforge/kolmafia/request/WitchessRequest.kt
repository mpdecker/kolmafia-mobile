package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

open class WitchessRequest(private val client: HttpClient) {

    data class PuzzleResult(
        val puzzleId: Int,
        val isSolved: Boolean,
        val responseText: String,
    )

    private val puzzleNumberPattern = Regex("""Witchess Puzzle #(\d+)""")
    private val solvedPattern = Regex("Solved Today")

    open suspend fun visitPage(path: String): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/$path")
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun fetchPuzzle(
        num: String,
        preferences: Preferences,
        errorOnNoRun: Boolean = false,
    ): Result<PuzzleResult> {
        if (preferences.getInt("puzzleChampBonus", 5) == 20) {
            val message = "You have already solved all of the Witchess puzzles."
            return if (errorOnNoRun) Result.failure(Exception(message)) else Result.success(
                PuzzleResult(-1, false, message),
            )
        }
        if (preferences.getBoolean("_witchessBuff", false)) {
            val message = "You already solved today's Witchess puzzles."
            return if (errorOnNoRun) Result.failure(Exception(message)) else Result.success(
                PuzzleResult(-1, false, message),
            )
        }

        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/witchess.php",
                formParameters = parameters { append("num", num) },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val body = response.bodyAsText()
                val puzzleId = puzzleNumberPattern.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: -1
                val isSolved = solvedPattern.containsMatchIn(body)
                if (errorOnNoRun && puzzleId < 0) {
                    Result.failure(Exception("Could not determine Witchess puzzle number."))
                } else {
                    Result.success(PuzzleResult(puzzleId, isSolved, body))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun claimBuff(preferences: Preferences): Result<String> {
        if (preferences.getBoolean("_witchessBuff", false)) {
            return Result.success("You already got your Witchess buff today.")
        }
        if (preferences.getInt("puzzleChampBonus", 5) != 20) {
            return Result.success(
                "You cannot automatically get a Witchess buff until all puzzles are solved.",
            )
        }

        visitPage("campground.php?action=witchess").onFailure { return Result.failure(it) }
        visitPage("choice.php?whichchoice=1181&option=3").onFailure { return Result.failure(it) }

        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = parameters {
                    append("whichchoice", "1183")
                    append("option", "2")
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val body = response.bodyAsText()
                if (body.contains("Puzzle Champ")) {
                    preferences.setBoolean("_witchessBuff", true)
                    Result.success("You got your Witchess buff.")
                } else {
                    Result.success(body)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun submitSolution(puzzleId: Int, coords: String): Result<Boolean> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/witchess.php",
            formParameters = parameters {
                append("sol", coords)
                append("ajax", "1")
                append("number", puzzleId.toString())
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val body = response.bodyAsText()
            Result.success(body.startsWith("[true"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
