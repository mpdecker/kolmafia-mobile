package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.effect.EffectState

/** Desktop [net.sourceforge.kolmafia.KoLCharacter.TurtleBlessing]. */
enum class TurtleBlessing {
    WAR,
    STORM,
    SHE_WHO_WAS,
    ;

    companion object {
        private const val BLESSING_OF_THE_WAR_SNAPPER = 1416
        private const val GRAND_BLESSING_OF_THE_WAR_SNAPPER = 1417
        private const val GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER = 1418
        private const val BLESSING_OF_SHE_WHO_WAS = 1419
        private const val GRAND_BLESSING_OF_SHE_WHO_WAS = 1420
        private const val GLORIOUS_BLESSING_OF_SHE_WHO_WAS = 1421
        private const val BLESSING_OF_THE_STORM_TORTOISE = 1422
        private const val GRAND_BLESSING_OF_THE_STORM_TORTOISE = 1423
        private const val GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE = 1424
        private const val AVATAR_OF_THE_WAR_SNAPPER = 1432
        private const val AVATAR_OF_SHE_WHO_WAS = 1433
        private const val AVATAR_OF_THE_STORM_TORTOISE = 1434

        /** Desktop [net.sourceforge.kolmafia.KoLCharacter.getBlessingType]. */
        fun fromActiveEffects(effectState: EffectState): TurtleBlessing? {
            val activeIds = effectState.effects.map { it.id }.toSet()
            if (BLESSING_OF_THE_WAR_SNAPPER in activeIds ||
                GRAND_BLESSING_OF_THE_WAR_SNAPPER in activeIds ||
                GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER in activeIds ||
                AVATAR_OF_THE_WAR_SNAPPER in activeIds
            ) {
                return WAR
            }
            if (BLESSING_OF_SHE_WHO_WAS in activeIds ||
                GRAND_BLESSING_OF_SHE_WHO_WAS in activeIds ||
                GLORIOUS_BLESSING_OF_SHE_WHO_WAS in activeIds ||
                AVATAR_OF_SHE_WHO_WAS in activeIds
            ) {
                return SHE_WHO_WAS
            }
            if (BLESSING_OF_THE_STORM_TORTOISE in activeIds ||
                GRAND_BLESSING_OF_THE_STORM_TORTOISE in activeIds ||
                GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE in activeIds ||
                AVATAR_OF_THE_STORM_TORTOISE in activeIds
            ) {
                return STORM
            }
            return null
        }
    }
}
