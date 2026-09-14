package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop residual legacy coinmaster [accessible] gates
 * (HTTP Residual LI Track B: AWOL / Big Brother / Dino / Crimbo17 / Skeleton / Altar).
 */
object ResidualLegacyShopAccessibility {

    const val AWOL_COMMENDATION = 5116
    const val BUBBLIN_STONE = 3605
    const val CRYSTALLINE_CHEER = 9625
    const val SKELETON_OF_CRIMBO_PAST = 326

    private val SELF_SCUBA = listOf(
        3607, // aerated diving helmet
        4285, // scholar mask
        4284, // gladiator mask
        4282, // crappy mask
        734, // scuba gear
        6315, // old scuba tank
    )
    private val FAMILIAR_SCUBA = listOf(
        4229, // amphibious tophat
        3609, // das boot
        3470, // bathysphere
    )

    fun awolInaccessible(accessibleCount: (Int) -> Int): String? =
        if (accessibleCount(AWOL_COMMENDATION) > 0) null
        else "You don't have any A. W. O. L. commendations"

    fun bigBrotherInaccessible(
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        adventureUnderwater: Boolean = false,
        underwaterFamiliar: Boolean = false,
    ): String? {
        val rescued = prefs?.getBoolean("bigBrotherRescued", false) == true ||
            accessibleCount(BUBBLIN_STONE) > 0
        if (!rescued) return "You haven't rescued Big Brother yet."
        val hasSelfGear = adventureUnderwater || SELF_SCUBA.any { accessibleCount(it) > 0 }
        if (!hasSelfGear) {
            return "You don't have the right equipment to adventure underwater."
        }
        val hasFamiliarGear = underwaterFamiliar || FAMILIAR_SCUBA.any { accessibleCount(it) > 0 }
        if (!hasFamiliarGear) {
            return "Your familiar doesn't have the right equipment to adventure underwater."
        }
        return null
    }

    fun dinostaurInaccessible(char: CharacterState): String? =
        if (char.inDinocore) null else "Dino World is not available"

    fun crimbo17Inaccessible(accessibleCount: (Int) -> Int): String? =
        if (accessibleCount(CRYSTALLINE_CHEER) > 0) null
        else "You need some crystalline cheer."

    fun skeletonOfCrimboPastInaccessible(ownsFamiliar: (Int) -> Boolean): String? =
        if (ownsFamiliar(SKELETON_OF_CRIMBO_PAST)) null
        else "You do not have a Skeleton of Crimbo Past"

    fun altarOfBonesInaccessible(): String =
        "The Altar of Bones is not available"
}
