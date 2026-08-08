package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.DesertBeachAccessibility

/** Desktop cafe visit gates for queue purchase preflight. */
object CafeAccessibility {

    private const val LARP_MEMBERSHIP_CARD_ID = 3506

    fun isHellKitchenAvailable(state: CharacterState?): Boolean {
        if (state == null) return true
        return ZodiacSign.find(state.zodiacSign)?.isBadMoon == true
    }

    fun isChezSnooteeAvailable(state: CharacterState?): Boolean {
        if (state == null) return true
        if (state.inZombiecore) return false
        if (state.ascensionPath == AscensionPath.AVATAR_OF_JARLSBERG) return false
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT ||
            sign == ZodiacSign.VOLE
    }

    fun isMicroBreweryAvailable(state: CharacterState?, prefs: Preferences?): Boolean {
        if (state == null) return true
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        if (sign != ZodiacSign.WOMBAT &&
            sign != ZodiacSign.BLENDER &&
            sign != ZodiacSign.PACKRAT
        ) {
            return false
        }
        if (!DesertBeachAccessibility.isAvailable(state, prefs)) return false
        if (state.isKingdomOfExploathing) return false
        return true
    }

    fun discountedPrice(price: Int, larpCount: Int = 0): Int {
        if (larpCount <= 0) return price
        return kotlin.math.ceil(0.90 * price.toDouble()).toInt()
    }

    fun larpCount(inventoryCountById: (Int) -> Int): Int =
        inventoryCountById(LARP_MEMBERSHIP_CARD_ID)
}
