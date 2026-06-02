package net.sourceforge.kolmafia.skill

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class SkillCastRequest(private val client: HttpClient) {

    suspend fun cast(skillId: Int, quantity: Int = 1): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/skills.php",
            formParameters = parameters {
                append("action", "useskill")
                append("whichskill", skillId.toString())
                append("quantity", quantity.toString())
            }
        )
        val body = response.bodyAsText()
        when {
            body.contains("don't have enough", ignoreCase = true) ||
            body.contains("not enough mp", ignoreCase = true) ->
                Result.failure(Exception("Not enough MP"))
            body.contains("daily limit", ignoreCase = true) ->
                Result.failure(Exception("Daily limit reached"))
            else -> Result.success(body)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
