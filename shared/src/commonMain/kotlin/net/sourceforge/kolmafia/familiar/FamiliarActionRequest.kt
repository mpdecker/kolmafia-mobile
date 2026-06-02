package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FamiliarActionRequest(private val client: HttpClient) {
    suspend fun perform(action: FamiliarAction): Result<Unit> = when (action) {
        is FamiliarAction.PocketProfessorLecture -> try {
            client.submitForm(
                url = "$KOL_BASE_URL/familiar.php",
                formParameters = parameters {
                    append("action", "lecture")
                    append("lectureid", action.lectureId.toString())
                }
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }

        is FamiliarAction.ShortestWigAssignment -> try {
            client.submitForm(
                url = "$KOL_BASE_URL/familiar.php",
                formParameters = parameters {
                    append("action", "wig")
                    append("colorid", action.colorId.toString())
                }
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }

        is FamiliarAction.Unsupported ->
            Result.failure(UnsupportedOperationException(
                "No mobile support for familiar action: ${action.rawType}. Use the KoL web interface."
            ))
    }
}
