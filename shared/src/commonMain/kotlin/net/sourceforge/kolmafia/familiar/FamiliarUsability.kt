package net.sourceforge.kolmafia.familiar

import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.session.YouRobotManager

/** Desktop [KoLCharacter.isUsable] / [KoLCharacter.usableFamiliar] parity. */
object FamiliarUsability {

    private val graftedFamiliarPrefs = listOf(
        "zootGraftedButtCheekLeftFamiliar",
        "zootGraftedButtCheekRightFamiliar",
        "zootGraftedFootLeftFamiliar",
        "zootGraftedFootRightFamiliar",
        "zootGraftedHandLeftFamiliar",
        "zootGraftedHandRightFamiliar",
        "zootGraftedHeadFamiliar",
        "zootGraftedNippleLeftFamiliar",
        "zootGraftedNippleRightFamiliar",
        "zootGraftedShoulderLeftFamiliar",
        "zootGraftedShoulderRightFamiliar",
    )

    fun isUsable(
        familiar: FamiliarData,
        state: CharacterState?,
        preferences: Preferences? = null,
    ): Boolean {
        if (state == null) return true

        val definition = FamiliarDefinitionDatabase.getById(familiar.id)

        if (state.inPokefam && definition?.isPokefamType == true) {
            return true
        }

        if (!StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, familiar.race, state)) {
            return false
        }

        if (state.inZombiecore && definition?.isUndead != true) {
            return false
        }

        if (state.inBeecore && Beeosity.hasBeeosity(familiar.race)) {
            return false
        }

        if (state.inGLover && !Beeosity.hasGs(familiar.race)) {
            return false
        }

        if (state.inZootomist && isGraftedFamiliar(familiar.id, preferences)) {
            return false
        }

        return true
    }

    fun usableByRace(
        familiarState: FamiliarState,
        race: String,
        characterState: CharacterState?,
        preferences: Preferences? = null,
    ): FamiliarData? {
        if (characterState != null && !characterState.ascensionPath.canUseFamiliars()) {
            return null
        }
        if (characterState?.inRobocore == true && !YouRobotManager.canUseFamiliars()) {
            return null
        }
        if (characterState?.inQuantum == true) {
            val active = familiarState.activeFamiliar ?: return null
            return active.takeIf {
                it.race.equals(race, ignoreCase = true) &&
                    isUsable(it, characterState, preferences)
            }
        }
        return familiarState.ownedFamiliars
            .firstOrNull { it.race.equals(race, ignoreCase = true) }
            ?.takeIf { isUsable(it, characterState, preferences) }
    }

    fun firstUsableFromGoals(
        familiarState: FamiliarState,
        races: List<String>,
        characterState: CharacterState?,
        preferences: Preferences? = null,
    ): String? {
        for (race in races) {
            if (race.equals("none", ignoreCase = true)) continue
            if (usableByRace(familiarState, race, characterState, preferences) != null) {
                return race
            }
        }
        return null
    }

    private fun isGraftedFamiliar(familiarId: Int, preferences: Preferences?): Boolean {
        if (preferences == null || familiarId <= 0) return false
        return graftedFamiliarPrefs.any { preferences.getInt(it) == familiarId }
    }
}
