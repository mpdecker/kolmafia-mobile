package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState

/**
 * Desktop legacy / catalog coinmaster [accessible] gates
 * (HTTP Residual L Track B: Mr Store 2002 / BURT / Fudge / Hermit / Game Shoppe).
 */
object LegacyCoinmasterAccessibility {

    const val MR_STORE_2002_CATALOG = 11257
    const val REPLICA_MR_STORE_2002_CATALOG = 11280
    const val BURT = 5683
    const val FUDGECULE = 5435
    const val FUDGE_WAND = 5441

    fun mrStore2002Inaccessible(
        char: CharacterState,
        accessibleCount: (Int) -> Int,
    ): String? =
        if (catalogToUse(char, accessibleCount) > 0) null
        else "You need a 2002 Mr. Store Catalog in order to shop here."

    fun catalogToUse(char: CharacterState, accessibleCount: (Int) -> Int): Int {
        if (accessibleCount(MR_STORE_2002_CATALOG) > 0) return MR_STORE_2002_CATALOG
        if (char.inLegacyOfLoathing && accessibleCount(REPLICA_MR_STORE_2002_CATALOG) > 0) {
            return REPLICA_MR_STORE_2002_CATALOG
        }
        return 0
    }

    fun burtInaccessible(accessibleCount: (Int) -> Int): String? =
        if (accessibleCount(BURT) > 0) null else "You don't have any BURTs"

    fun fudgeWandInaccessible(accessibleCount: (Int) -> Int): String? {
        if (accessibleCount(FUDGE_WAND) <= 0) return "You don't have a wand of fudge control"
        if (accessibleCount(FUDGECULE) <= 0) return "You don't have any fudgecules"
        return null
    }

    fun hermitInaccessible(char: CharacterState): String? =
        if (char.isKingdomOfExploathing) "The Hermitage exploded" else null

    fun gameShoppeInaccessible(char: CharacterState): String? =
        if (char.isHardcore || char.isInRonin) {
            "Characters in Hardcore or Ronin cannot redeem Game Shoppe credit."
        } else {
            null
        }
}
