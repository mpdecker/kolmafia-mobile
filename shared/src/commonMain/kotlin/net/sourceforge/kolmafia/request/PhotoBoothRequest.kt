package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop PhotoBoothCommand effect branch — choices 1533 / 1534. */
class PhotoBoothRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeEffect(
        effectArg: String,
        preferences: Preferences?,
    ): Result<String> {
        val choice = findEffectOption(effectArg)
            ?: return Result.failure(
                IllegalArgumentException("I don't understand what effect $effectArg is."),
            )
        val preflight = preflightError(preferences)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }

        return try {
            val visit = client.get("$KOL_BASE_URL/clan_viplounge.php?action=photobooth")
            if (!visit.status.isSuccess()) {
                return Result.failure(IllegalStateException("Could not visit photo booth."))
            }
            choiceRequest.choose(MENU_CHOICE, 1).onFailure { return Result.failure(it) }
            choiceRequest.choose(EFFECT_CHOICE, choice).onFailure { return Result.failure(it) }
            choiceRequest.choose(MENU_CHOICE, 6).onFailure { return Result.failure(it) }
            if (preferences != null) {
                preferences.setInt(
                    EFFECTS_PREF,
                    preferences.getInt(EFFECTS_PREF, 0) + 1,
                )
            }
            Result.success("ok")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val MENU_CHOICE = 1533
        const val EFFECT_CHOICE = 1534
        const val EFFECTS_PREF = "_photoBoothEffects"

        fun findEffectOption(parameter: String): Int? {
            val p = parameter.trim().lowercase()
            return when {
                p.startsWith("wild") -> 1
                p.startsWith("tower") -> 2
                p.startsWith("space") -> 3
                else -> null
            }
        }

        fun preflightError(preferences: Preferences?): String? {
            if (!ClanLoungeSync.hasPhotoBooth(preferences)) {
                return "Your clan needs a photo booth."
            }
            val effects = preferences?.getInt(EFFECTS_PREF, 0) ?: 0
            if (effects >= 3) {
                return "You cannot get any more effects."
            }
            return null
        }
    }
}
