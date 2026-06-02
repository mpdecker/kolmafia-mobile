package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class CharacterRequest(private val client: HttpClient) {

    suspend fun fetchCharacterState(): Result<CharacterApiResponse> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "status")
                parameter("for", "KoLmafia-Mobile")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
