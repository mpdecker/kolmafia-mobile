package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BeachHeadAvailability

/** Desktop [BeachCombRequest] — Beach Comb head buffs (choice 1388). */
class BeachCombRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun combHead(
        query: String,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val head = BeachHeadAvailability.resolveHead(query)
            ?: return Result.failure(
                IllegalArgumentException("Which beach head is $query?"),
            )
        val hasComb = inventoryCounts(BeachHeadAvailability.BEACH_COMB_ID) > 0 ||
            inventoryCounts(BeachHeadAvailability.DRIFTWOOD_BEACH_COMB_ID) > 0
        if (!hasComb) {
            return Result.failure(
                IllegalStateException("You need either a Beach Comb or a driftwood beach comb"),
            )
        }
        if (head.id in BeachHeadAvailability.parseBeachHeadsUsed(prefs)) {
            return Result.failure(
                IllegalStateException("You've already combed beach head #${head.id}"),
            )
        }
        return try {
            val visit = client.get("$KOL_BASE_URL/main.php") {
                parameter("comb", "1")
            }
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Beach Comb visit failed."))
            }
            val unlocked = BeachHeadAvailability.parseBeachHeadsUnlocked(prefs)
            val html = if (head.id in unlocked) {
                choiceRequest.choose(
                    CHOICE_ID,
                    HEAD_OPTION,
                    mapOf("buff" to head.id.toString()),
                ).getOrElse { return Result.failure(it) }.first
            } else {
                choiceRequest.choose(
                    CHOICE_ID,
                    WANDER_OPTION,
                    mapOf("minutes" to head.beach.toString()),
                ).exceptionOrNull()?.let { return Result.failure(it) }
                choiceRequest.choose(
                    CHOICE_ID,
                    COMB_OPTION,
                    mapOf("coords" to head.coords),
                ).getOrElse { return Result.failure(it) }.first
            }
            choiceRequest.choose(CHOICE_ID, EXIT_OPTION)
            BeachHeadAvailability.markHeadUsed(prefs, head.id)
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CHOICE_ID = 1388
        const val WANDER_OPTION = 1
        const val HEAD_OPTION = 3
        const val COMB_OPTION = 4
        const val EXIT_OPTION = 5

        /** Parse `head <query>` from `beach head …` CLI parameters (after "beach"). */
        fun parseHeadQuery(parameters: String): String? {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) return null
            val lower = trimmed.lowercase()
            if (!lower.startsWith("head")) return null
            return trimmed.substringAfter("head").trim().ifEmpty { null }
        }
    }
}
