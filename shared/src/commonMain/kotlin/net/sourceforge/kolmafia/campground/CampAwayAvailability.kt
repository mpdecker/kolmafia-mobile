package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest

/** Desktop [CampAwayRequest.campAwayTentAvailable] gate for Camp Away tent buffs. */
object CampAwayAvailability {

    fun campAwayTentAvailable(charState: CharacterState, prefs: Preferences?): Boolean {
        if (prefs?.getBoolean("getawayCampsiteUnlocked", false) != true) return false
        if (!StandardRequest.isAllowed(
                RestrictedItemType.ITEMS,
                "Distant Woods Getaway Brochure",
                charState,
            )
        ) {
            return false
        }
        if (charState.zodiacSign.equals("Bad Moon", ignoreCase = true)) return false
        if (LimitModeGates.limitZone("Woods", charState.limitMode)) return false
        return true
    }
}
