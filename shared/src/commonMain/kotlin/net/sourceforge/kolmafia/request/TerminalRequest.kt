package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [TerminalRequest] — visit Source terminal + POST choice 1191 command input. */
class TerminalRequest(
    private val client: HttpClient,
    private val campgroundRequest: CampgroundRequest,
    private val falloutShelterRequest: FalloutShelterRequest,
) {
    suspend fun extrude(
        command: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        if (!hasTerminal(state, preferences, accessibleCount)) {
            return Result.failure(IllegalStateException("You don't have a Source terminal."))
        }
        val visit = if (state?.inNuclearAutumn == true) {
            falloutShelterRequest.visitTerminal()
        } else {
            campgroundRequest.visitTerminal()
        }
        visit.exceptionOrNull()?.let { return Result.failure(it) }
        return postTerminalCommand(command)
    }

    private suspend fun postTerminalCommand(input: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = terminalCommandForm(input),
        )
        Result.success(response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val SOURCE_TERMINAL = 9033
        private const val REPLICA_SOURCE_TERMINAL = 11231
        private const val TERMINAL_CHOICE = 1191

        internal fun hasTerminal(
            state: CharacterState?,
            preferences: Preferences?,
            accessibleCount: (Int) -> Int,
        ): Boolean {
            if (CampgroundItemSync.hasSourceTerminal(preferences)) return true
            if (accessibleCount(SOURCE_TERMINAL) > 0) return true
            if (state?.inLegacyOfLoathing == true && accessibleCount(REPLICA_SOURCE_TERMINAL) > 0) {
                return true
            }
            return state?.inNuclearAutumn == true && accessibleCount(SOURCE_TERMINAL) > 0
        }

        internal fun terminalCommandForm(input: String): Parameters =
            parameters {
                append("whichchoice", TERMINAL_CHOICE.toString())
                append("option", "1")
                append("input", input)
            }
    }
}
