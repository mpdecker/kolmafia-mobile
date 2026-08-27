package net.sourceforge.kolmafia.request

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class CharacterRequest(val client: HttpClient) {

    suspend fun fetchCharacterStatusRaw(): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "status")
                parameter("for", "KoLmafia-Mobile")
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCharacterState(): Result<CharacterApiResponse> {
        return fetchCharacterStatusRaw().mapCatching { raw ->
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                .decodeFromString(CharacterApiResponse.serializer(), raw)
        }
    }

    suspend fun fetchCharpaneHtml(): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/charpane.php")
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
