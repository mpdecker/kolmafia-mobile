package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.concoction.BarrelShrineRequest] — da.php + choice 1100. */
class BarrelCreateRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val preferences: Preferences? = null,
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        val cappedQuantity = quantity.coerceAtMost(1)
        if (cappedQuantity <= 0) return Result.success(0)

        if (concoction.ingredients.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("BARREL recipe for '${concoction.result}' is invalid."),
            )
        }

        val prefs = preferences ?: this.preferences
        val option = BarrelChoiceMapper.optionFor(concoction.result)
            ?: return Result.failure(IllegalStateException("Unknown BARREL result: ${concoction.result}"))

        if (state?.isKingdomOfExploathing == true) {
            return Result.failure(
                IllegalStateException("The barrel shrine has been blown to smithereens."),
            )
        }

        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = prefs,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("BARREL craft not permitted: ${concoction.result}"))
        }

        if (prefs != null && !BarrelChoiceMapper.availableBarrelItem(concoction.result, prefs)) {
            return Result.failure(
                IllegalStateException("BARREL item not available this ascension/day: ${concoction.result}"),
            )
        }

        return try {
            val visit = client.get("$DA_BARREL_SHRINE_URL")
            if (!visit.status.isSuccess()) {
                return Result.success(0)
            }

            val choiceResult = choiceRequest.choose(BarrelChoiceMapper.CHOICE_ID, option)
            if (choiceResult.isFailure) {
                return Result.success(0)
            }

            val body = choiceResult.getOrThrow().first
            if (!isSuccessResponse(body)) {
                return Result.success(0)
            }

            prefs?.let { BarrelChoiceMapper.applySuccessPrefs(concoction.result, it) }
            ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
            Result.success(1)
        } catch (_: Exception) {
            Result.success(0)
        }
    }

    private fun isSuccessResponse(body: String): Boolean =
        body.contains("You acquire", ignoreCase = true)

    companion object {
        private const val DA_BARREL_SHRINE_URL = "https://www.kingdomofloathing.com/da.php?barrelshrine=1"
    }
}
