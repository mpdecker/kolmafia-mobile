package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FamiliarRequest(private val client: HttpClient) {
    suspend fun switchFamiliar(familiarId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/familiar.php",
            formParameters = parameters {
                append("action", "newfam")
                append("whichfam", familiarId.toString())
            }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun putBack(): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/familiar.php",
            formParameters = parameters { append("action", "putback") }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Enthrone a familiar in the Crown of Thrones (0 = clear). */
    suspend fun enthrone(familiarId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/familiar.php",
            formParameters = parameters {
                append("action", "hatseat")
                append("famid", familiarId.toString())
                append("ajax", "1")
            }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Bjornify a familiar in Buddy Bjorn (0 = clear). */
    suspend fun bjornify(familiarId: Int): Result<Unit> = try {
        client.submitForm(
            url = "$KOL_BASE_URL/familiar.php",
            formParameters = parameters {
                append("action", "backpack")
                append("famid", familiarId.toString())
                append("ajax", "1")
            }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
