package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.effect.EffectState

/** Desktop [net.sourceforge.kolmafia.KoLCharacter.TurtleBlessingLevel]. */
enum class TurtleBlessingLevel {
    PARIAH,
    NONE,
    BLESSING,
    GRAND_BLESSING,
    GLORIOUS_BLESSING,
    AVATAR,
    ;

    fun boonDuration(): Int = when (this) {
        BLESSING -> 5
        GRAND_BLESSING -> 10
        GLORIOUS_BLESSING -> 15
        else -> 0
    }

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
        private const val SPIRIT_PARIAH = 1431
        private const val AVATAR_OF_THE_WAR_SNAPPER = 1432
        private const val AVATAR_OF_SHE_WHO_WAS = 1433
        private const val AVATAR_OF_THE_STORM_TORTOISE = 1434

        /** Desktop [net.sourceforge.kolmafia.KoLCharacter.getBlessingLevel]. */
        fun fromActiveEffects(effectState: EffectState): TurtleBlessingLevel {
            val activeIds = effectState.effects.map { it.id }.toSet()
            if (BLESSING_OF_THE_WAR_SNAPPER in activeIds ||
                BLESSING_OF_SHE_WHO_WAS in activeIds ||
                BLESSING_OF_THE_STORM_TORTOISE in activeIds
            ) {
                return BLESSING
            }
            if (GRAND_BLESSING_OF_THE_WAR_SNAPPER in activeIds ||
                GRAND_BLESSING_OF_SHE_WHO_WAS in activeIds ||
                GRAND_BLESSING_OF_THE_STORM_TORTOISE in activeIds
            ) {
                return GRAND_BLESSING
            }
            if (GLORIOUS_BLESSING_OF_THE_WAR_SNAPPER in activeIds ||
                GLORIOUS_BLESSING_OF_SHE_WHO_WAS in activeIds ||
                GLORIOUS_BLESSING_OF_THE_STORM_TORTOISE in activeIds
            ) {
                return GLORIOUS_BLESSING
            }
            if (AVATAR_OF_THE_WAR_SNAPPER in activeIds ||
                AVATAR_OF_SHE_WHO_WAS in activeIds ||
                AVATAR_OF_THE_STORM_TORTOISE in activeIds
            ) {
                return AVATAR
            }
            if (SPIRIT_PARIAH in activeIds) {
                return PARIAH
            }
            return NONE
        }
    }
}
