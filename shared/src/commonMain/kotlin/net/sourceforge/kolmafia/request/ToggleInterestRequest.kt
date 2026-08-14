package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.http.KOL_BASE_URL

/** Desktop ToggleCommand — charsheet.php action=newyouinterest. */
class ToggleInterestRequest(
    private val client: HttpClient,
) {
    suspend fun toggle(effectState: EffectState?): Result<String> {
        val err = preflightError(effectState)
        if (err != null) {
            return Result.failure(IllegalStateException(err))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/charsheet.php",
                formParameters = parameters {
                    append("action", "newyouinterest")
                },
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Toggle interest failed."))
            }
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SUPERFICIALLY_INTERESTED = 2288
        const val INTENSELY_INTERESTED = 2289

        fun preflightError(effectState: EffectState?): String? {
            val effects = effectState?.effects.orEmpty()
            val has = effects.any {
                it.id == SUPERFICIALLY_INTERESTED || it.id == INTENSELY_INTERESTED
            }
            return if (has) null else "You don't have an effect to toggle."
        }
    }
}
