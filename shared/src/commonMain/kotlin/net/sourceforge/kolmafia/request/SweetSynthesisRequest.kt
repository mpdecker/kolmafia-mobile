package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [SweetSynthesisRequest] — Sweet Synthesis effect via candy pair. */
class SweetSynthesisRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val retrieveItemService: RetrieveItemService? = null,
) {
    suspend fun synthesize(
        effectQuery: String,
        preferences: Preferences?,
        charState: CharacterState?,
        inventoryCounts: (Int) -> Int,
        hasSkill: Boolean,
    ): Result<String> {
        if (!hasSkill) {
            return Result.failure(IllegalStateException("You need the Sweet Synthesis skill."))
        }
        if (charState?.inGLover == true) {
            return Result.failure(IllegalStateException("You cannot synthesize in G-Lover."))
        }
        val spleenRemaining = charState?.spleenRemaining ?: 0
        if (spleenRemaining < 1) {
            return Result.failure(
                IllegalStateException("Your spleen has been abused enough today"),
            )
        }
        val effectId = resolveEffectId(effectQuery)
            ?: return Result.failure(
                IllegalArgumentException("Unknown synthesis effect: $effectQuery"),
            )
        CandyDatabase.loadBlacklist(preferences)
        val pair = CandyDatabase.synthesisPairIds(effectId, inventoryCounts)
        if (pair.size < 2) {
            return Result.failure(
                IllegalStateException("No available candy pair for that effect."),
            )
        }
        val itemId1 = pair[0]
        val itemId2 = pair[1]
        if (itemId1 == itemId2) {
            retrieveItemService?.retrieve(itemId1, 2)
        } else {
            retrieveItemService?.retrieve(itemId1, 1)
            retrieveItemService?.retrieve(itemId2, 1)
        }
        return try {
            val playerId = charState?.playerId ?: 0
            val skill = client.submitForm(
                url = "$KOL_BASE_URL/runskillz.php",
                formParameters = parameters {
                    append("action", "Skillz")
                    append("whichskill", SKILL_ID.toString())
                    append("targetplayer", playerId.toString())
                    append("quantity", "1")
                },
            )
            if (!skill.status.isSuccess()) {
                return Result.failure(IllegalStateException("Sweet Synthesis skill failed."))
            }
            choiceRequest.choose(
                CHOICE_ID,
                OPTION,
                mapOf(
                    "a" to itemId1.toString(),
                    "b" to itemId2.toString(),
                    "q" to "1",
                ),
            ).map { (html, _) -> html }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun synthesizePair(
        itemId1: Int,
        itemId2: Int,
        preferences: Preferences?,
        charState: CharacterState?,
        hasSkill: Boolean,
    ): Result<String> {
        if (!hasSkill) {
            return Result.failure(IllegalStateException("You need the Sweet Synthesis skill."))
        }
        if (charState?.inGLover == true) {
            return Result.failure(IllegalStateException("You cannot synthesize in G-Lover."))
        }
        val spleenRemaining = charState?.spleenRemaining ?: 0
        if (spleenRemaining < 1) {
            return Result.failure(
                IllegalStateException("Your spleen has been abused enough today"),
            )
        }
        if (itemId1 == itemId2) {
            retrieveItemService?.retrieve(itemId1, 2)
        } else {
            retrieveItemService?.retrieve(itemId1, 1)
            retrieveItemService?.retrieve(itemId2, 1)
        }
        return try {
            val playerId = charState?.playerId ?: 0
            val skill = client.submitForm(
                url = "$KOL_BASE_URL/runskillz.php",
                formParameters = parameters {
                    append("action", "Skillz")
                    append("whichskill", SKILL_ID.toString())
                    append("targetplayer", playerId.toString())
                    append("quantity", "1")
                },
            )
            if (!skill.status.isSuccess()) {
                return Result.failure(IllegalStateException("Sweet Synthesis skill failed."))
            }
            choiceRequest.choose(
                CHOICE_ID,
                OPTION,
                mapOf(
                    "a" to itemId1.toString(),
                    "b" to itemId2.toString(),
                    "q" to "1",
                ),
            ).map { (html, _) -> html }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SKILL_ID = 166
        const val CHOICE_ID = 1217
        const val OPTION = 1

        private val EFFECTS = listOf(
            "Synthesis: Hot" to 2165,
            "Synthesis: Cold" to 2166,
            "Synthesis: Pungent" to 2167,
            "Synthesis: Scary" to 2168,
            "Synthesis: Greasy" to 2169,
            "Synthesis: Strong" to 2170,
            "Synthesis: Smart" to 2171,
            "Synthesis: Cool" to 2172,
            "Synthesis: Hardy" to 2173,
            "Synthesis: Energy" to 2174,
            "Synthesis: Greed" to 2175,
            "Synthesis: Collection" to 2176,
            "Synthesis: Movement" to 2177,
            "Synthesis: Learning" to 2178,
            "Synthesis: Style" to 2179,
        )

        fun resolveEffectId(query: String): Int? {
            val q = query.trim()
            if (q.isEmpty()) return null
            q.toIntOrNull()?.let { id ->
                if (EFFECTS.any { it.second == id }) return id
            }
            val canonical = q.lowercase()
            EFFECTS.firstOrNull { it.first.equals(q, ignoreCase = true) }?.let { return it.second }
            val matches = EFFECTS.filter {
                it.first.lowercase().contains(canonical) ||
                    it.first.lowercase().removePrefix("synthesis: ").startsWith(canonical)
            }
            return matches.singleOrNull()?.second
        }

        /** Strip leading `synthesize` from CLI parameters. */
        fun parseEffectQuery(parameters: String): String? {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) return null
            return if (trimmed.lowercase().startsWith("synthesize")) {
                trimmed.substringAfter("synthesize").trim().ifEmpty { null }
            } else {
                trimmed
            }
        }
    }
}
