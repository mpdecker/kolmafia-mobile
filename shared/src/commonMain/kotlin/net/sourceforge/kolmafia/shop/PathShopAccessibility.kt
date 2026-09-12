package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.campground.CampAwayAvailability
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop path/pref coinmaster [accessible] gates (Behavioral Deepen XLIX Track A).
 * Fancy Dan / Exploathing / Pokéfam / Nuclear / Plumber / Ed / Precinct / Rumple /
 * Spacegate / Campfire / Boutique.
 */
object PathShopAccessibility {

    const val ODD_SILVER_COIN = 7144

    fun fancyDanInaccessible(prefs: Preferences?): String? =
        if (prefs?.getBoolean("ownsSpeakeasy", false) == true) null
        else "You don't own a speakeasy"

    fun cosmicRaysInaccessible(char: CharacterState): String? =
        if (char.isKingdomOfExploathing) null else "The Kingdom is not Exploathing"

    fun pokemporiumInaccessible(char: CharacterState): String? =
        if (char.inPokefam) null else "You're not in PokeFam"

    fun geneticFiddlingInaccessible(char: CharacterState): String? =
        if (char.inNuclearAutumn) null else "You don't have a Fallout Shelter"

    fun plumberInaccessible(char: CharacterState): String? =
        if (isPlumber(char)) null else "You are not a plumber."

    fun edShopInaccessible(char: CharacterState): String? {
        // fromApiString resolves "Actually Ed the Undying" to AscensionPath.ED
        // (same apiName/pathId as ACTUALLY_ED_THE_UNDYING; maxBy keeps the earlier enum).
        if (char.ascensionPath != AscensionPath.ED &&
            char.ascensionPath != AscensionPath.ACTUALLY_ED_THE_UNDYING
        ) {
            return "Only Ed can come here."
        }
        if (!char.limitMode.equals("ed", ignoreCase = true) &&
            !char.limitMode.equals("edunder", ignoreCase = true)
        ) {
            return "You must be in the Underworld to shop here."
        }
        return null
    }

    fun precinctInaccessible(prefs: Preferences?): String? =
        if (prefs?.getBoolean("hasDetectiveSchool", false) == true) null
        else "You cannot access the Precinct"

    fun rumpleInaccessible(prefs: Preferences?): String? =
        if (prefs?.getString("grimstoneMaskPath", "").equals("gnome", ignoreCase = true) == true) {
            null
        } else {
            "You need access to Rumplestiltskin's Workshop to make that."
        }

    fun spacegateInaccessible(prefs: Preferences?): String? {
        if (prefs?.getBoolean("_spacegateToday", false) == true ||
            prefs?.getBoolean("spacegateAlways", false) == true
        ) {
            return null
        }
        return "You can't get to the Spacegate."
    }

    fun campfireInaccessible(char: CharacterState, prefs: Preferences?): String? =
        if (CampAwayAvailability.campAwayTentAvailable(char, prefs)) null
        else "Need access to your Getaway Campsite"

    fun boutiqueInaccessible(accessibleCount: (Int) -> Int): String? =
        if (accessibleCount(ODD_SILVER_COIN) > 0) null
        else "You don't have an odd silver coin."

    private fun isPlumber(char: CharacterState): Boolean =
        char.ascensionPath == AscensionPath.PLUMBER ||
            char.ascensionPath == AscensionPath.PATH_OF_THE_PLUMBER
}
