package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.LimitModeGates

/** Desktop [CampgroundRequest.haveCampground] path/limit-mode gates. */
object CampgroundAvailability {

    fun haveCampground(state: CharacterState): Boolean {
        if (LimitModeGates.limitCampground(state.limitMode)) return false
        if (state.isActuallyEd ||
            state.inRobocore ||
            state.inNuclearAutumn ||
            state.inSmallcore ||
            state.inWereProfessor ||
            state.isMeat
        ) {
            return false
        }
        return true
    }
}
