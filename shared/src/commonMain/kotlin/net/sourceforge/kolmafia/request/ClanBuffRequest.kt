package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class ClanBuffRequest(private val client: HttpClient) {
    data class Buff(val id: Int, val name: String)

    open suspend fun buyBuff(buffId: Int): Result<String> {
        if (buffId !in requestList.map { it.id }) {
            return Result.failure(IllegalArgumentException("Unknown clan buff: $buffId"))
        }
        return try {
            val response = client.submitForm(
                "$KOL_BASE_URL/clan_stash.php",
                parameters {
                    append("action", "buyround")
                    append("size", (buffId % 10).toString())
                    append("whichgift", (buffId / 10).toString())
                },
            )
            if (response.status.isSuccess()) Result.success(response.bodyAsText())
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val gifts = listOf(
            "Muscle Training",
            "Mysticality Training",
            "Moxie Training",
            "Temporary Muscle Boost",
            "Temporary Mysticality Boost",
            "Temporary Moxie Boost",
            "Temporary Item Drop Boost",
            "Temporary Meat Drop Boost",
        )
        private val sizes = listOf("Cheap", "Normal", "Expensive")
        val requestList: List<Buff> = buildList {
            gifts.forEachIndexed { giftIndex, gift ->
                sizes.forEachIndexed { sizeIndex, size ->
                    add(Buff((giftIndex + 1) * 10 + sizeIndex + 1, "$size $gift"))
                }
            }
            add(Buff(91, "Adventure Massage"))
        }
    }
}
