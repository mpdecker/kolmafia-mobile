package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop specialty IoTM / zone coinmaster [accessible] gates
 * (HTTP Residual L Track A: Shadow Forge / Fantasy Realm / Dedigitizer / Sea / Topiary).
 */
object SpecialtyShopAccessibility {

    const val TOPIARY_NUGGLET = 7968

    fun shadowForgeInaccessible(char: CharacterState, prefs: Preferences?): String? {
        val unlocked = prefs?.getInt("lastShadowForgeUnlockAdventure", -1) ?: -1
        return if (unlocked == char.currentRun) null
        else "You need to be at The Shadow Forge to make that."
    }

    fun fantasyRealmInaccessible(prefs: Preferences?): String? =
        if (prefs?.getBoolean("frAlways", false) == true ||
            prefs?.getBoolean("_frToday", false) == true
        ) {
            null
        } else {
            "Need access to Fantasy Realm"
        }

    fun dedigitizerInaccessible(prefs: Preferences?): String? =
        if (prefs?.getBoolean("crAlways", false) == true ||
            prefs?.getBoolean("_crToday", false) == true
        ) {
            null
        } else {
            "You can't access the server room."
        }

    fun sandPennyInaccessible(char: CharacterState): String? =
        if (char.inSeaPath) null
        else "You can't buy with sand pennies outside 11,037 Leagues Under the Sea"

    fun topiaryInaccessible(accessibleCount: (Int) -> Int): String? =
        if (accessibleCount(TOPIARY_NUGGLET) > 0) null
        else "You do not have a topiary nugglet in inventory"
}
