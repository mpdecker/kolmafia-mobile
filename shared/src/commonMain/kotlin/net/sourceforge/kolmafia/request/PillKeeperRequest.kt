package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop PillKeeperCommand — main.php?eowkeeper=1 + choice 1395. */
class PillKeeperRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    data class ResolveResult(
        val option: Int,
        val pillText: String,
        val wantFree: Boolean,
    )

    suspend fun takePills(
        parameters: String,
        state: CharacterState?,
        preferences: Preferences?,
        hasPillKeeper: Boolean,
    ): Result<String> {
        val resolved = resolve(parameters)
            ?: return Result.failure(IllegalArgumentException("Invalid choice"))
        val preflight = preflightError(resolved, state, preferences, hasPillKeeper)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }

        return try {
            val open = client.get("$KOL_BASE_URL/main.php?eowkeeper=1")
            if (!open.status.isSuccess()) {
                return Result.failure(IllegalStateException("Could not open pill keeper."))
            }
            val choice = choiceRequest.choose(CHOICE_ID, resolved.option)
            if (choice.isSuccess && preferences != null &&
                !preferences.getBoolean(FREE_PILL_PREF, false)
            ) {
                // First daily use consumes the free pill.
                preferences.setBoolean(FREE_PILL_PREF, true)
            }
            choice.map { it.first }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val CHOICE_ID = 1395
        const val PILL_KEEPER_ITEM_ID = 10333
        const val FREE_PILL_PREF = "_freePillKeeperUsed"

        fun resolve(parameters: String): ResolveResult? {
            val lower = parameters.trim().lowercase()
            if (lower.isEmpty()) return null
            val wantFree = Regex("""\bfree\b""").containsMatchIn(lower)
            val option = when {
                lower.contains("exp") -> 1 to "Monday - Explodinall"
                lower.contains("ext") -> 2 to "Tuesday - Extendicillin"
                lower.contains("non") -> 3 to "Wednesday - Sneakisol"
                lower.contains("ele") -> 4 to "Thursday - Rainbowolin"
                lower.contains("sta") -> 5 to "Friday - Hulkien"
                lower.contains("fam") -> 6 to "Saturday - Fidoxene"
                lower.contains("sem") || lower.contains("luc") -> 7 to "Sunday - Surprise Me"
                lower.contains("ran") -> 8 to "Funday - Telecybin"
                else -> return null
            }
            return ResolveResult(option.first, option.second, wantFree)
        }

        fun preflightError(
            resolved: ResolveResult,
            state: CharacterState?,
            preferences: Preferences?,
            hasPillKeeper: Boolean,
        ): String? {
            if (!hasPillKeeper) {
                return "You need an Eight Days a Week Pill Keeper"
            }
            val freeUsed = preferences?.getBoolean(FREE_PILL_PREF, false) == true
            if (state != null) {
                val spleenLeft = state.spleenLimit - state.spleenUsed
                if (spleenLeft < 3 && freeUsed) {
                    return "Your spleen has been abused enough today"
                }
            }
            if (resolved.wantFree && freeUsed) {
                return "Free pill keeper use already spent"
            }
            return null
        }
    }
}
