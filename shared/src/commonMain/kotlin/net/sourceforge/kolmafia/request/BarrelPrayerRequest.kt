package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BarrelShrineSync

/** Desktop [net.sourceforge.kolmafia.textui.command.BarrelPrayerCommand] — da.php + choice 1100. */
class BarrelPrayerRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun pray(option: Int, state: CharacterState?, prefs: Preferences?): Result<Boolean> {
        val preflight = preflightError(option, state, prefs)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }

        return try {
            val visit = client.get(DA_BARREL_SHRINE_URL)
            if (!visit.status.isSuccess()) {
                return Result.success(false)
            }

            val choiceResult = choiceRequest.choose(BarrelChoiceMapper.CHOICE_ID, option)
            if (choiceResult.isFailure) {
                return Result.success(false)
            }

            val body = choiceResult.getOrThrow().first
            val success = when (option) {
                BarrelChoiceMapper.OPTION_BUFF -> true
                else -> body.contains("You acquire", ignoreCase = true)
            }
            if (!success) {
                return Result.success(false)
            }

            if (prefs != null) {
                if (option == BarrelChoiceMapper.OPTION_BUFF) {
                    BarrelShrineSync.syncPostChoice(option, prefs)
                } else {
                    BarrelChoiceMapper.applyPrayerSuccess(option, prefs)
                }
            }
            if (option != BarrelChoiceMapper.OPTION_BUFF) {
                ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
            }
            Result.success(true)
        } catch (_: Exception) {
            Result.success(false)
        }
    }

    private fun preflightError(option: Int, state: CharacterState?, prefs: Preferences?): String? {
        if (prefs == null) return "Preferences are not available."
        if (!prefs.getBoolean("barrelShrineUnlocked", false)) {
            return "Barrel Shrine not installed"
        }
        if (state?.isKingdomOfExploathing == true) {
            return "The barrel shrine has been blown to smithereens"
        }
        if (state != null &&
            !StandardRequest.isAllowed(
                RestrictedItemType.ITEMS,
                "shrine to the Barrel god",
                state,
            )
        ) {
            return "Standard restrictions preclude you from approaching the Barrel Shrine"
        }
        if (prefs.getBoolean("_barrelPrayer", false)) {
            return "You have already prayed to the Barrel God today."
        }
        return when (option) {
            BarrelChoiceMapper.OPTION_PROTECTION ->
                if (prefs.getBoolean("prayedForProtection", false)) {
                    "You have already prayed for that item this ascension."
                } else {
                    null
                }
            BarrelChoiceMapper.OPTION_GLAMOUR ->
                if (prefs.getBoolean("prayedForGlamour", false)) {
                    "You have already prayed for that item this ascension."
                } else {
                    null
                }
            BarrelChoiceMapper.OPTION_VIGOR ->
                if (prefs.getBoolean("prayedForVigor", false)) {
                    "You have already prayed for that item this ascension."
                } else {
                    null
                }
            BarrelChoiceMapper.OPTION_BUFF -> null
            else -> "Unknown barrel prayer option."
        }
    }

    companion object {
        private const val DA_BARREL_SHRINE_URL = "https://www.kingdomofloathing.com/da.php?barrelshrine=1"
    }
}
