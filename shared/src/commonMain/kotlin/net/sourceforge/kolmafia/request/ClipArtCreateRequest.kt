package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.clipArtParams
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ClipArtRequest] — Summon Clip Art via campground combinecliparts. */
class ClipArtCreateRequest(
    private val client: HttpClient,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val (clip1, clip2, clip3) = concoction.clipArtParams()
            ?: return Result.failure(IllegalStateException("Missing clip art params for: ${concoction.result}"))
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("Clip art craft not permitted: ${concoction.result}"))
        }

        var created = 0
        repeat(quantity) {
            val response = postCombineClipArts(clip1, clip2, clip3)
            response.exceptionOrNull()?.let { return Result.failure(it) }
            val body = response.getOrThrow()
            if (!body.contains("You acquire")) {
                return Result.failure(IllegalStateException("Clip art creation was unsuccessful."))
            }
            recordClipArtSummon(state, preferences)
            created++
        }
        return Result.success(created)
    }

    private suspend fun postCombineClipArts(
        clip1: Int,
        clip2: Int,
        clip3: Int,
    ): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/campground.php",
            formParameters = clipArtForm(clip1, clip2, clip3),
        )
        Result.success(response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun recordClipArtSummon(state: CharacterState?, preferences: Preferences?) {
        val prefs = preferences ?: return
        val canInteract = state?.let { !it.isHardcore && !it.isInRonin } ?: true
        val prefName = if (canInteract) "_clipartSummons" else "tomeSummons"
        prefs.setInt(prefName, prefs.getInt(prefName, 0) + 1)
    }

    companion object {
        internal fun clipArtForm(clip1: Int, clip2: Int, clip3: Int): Parameters =
            parameters {
                append("preaction", "combinecliparts")
                append("clip1", clip1.toString())
                append("clip2", clip2.toString())
                append("clip3", clip3.toString())
            }
    }
}
