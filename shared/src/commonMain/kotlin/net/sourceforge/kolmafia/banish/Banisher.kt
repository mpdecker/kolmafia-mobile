// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/Banisher.kt
package net.sourceforge.kolmafia.banish

/**
 * How a banish is reset. Matches `BanishManager.Reset` in the desktop codebase.
 *
 * - [ROLLOVER]      — cleared every daily rollover (and on login, treated as rollover)
 * - [TURNS]         — cleared when the banish's turn count expires
 * - [TURN_ROLLOVER] — cleared when turns expire OR on rollover, whichever comes first
 * - [AVATAR]        — cleared on avatar/ascension change; treated as ROLLOVER here
 * - [NEVER]         — never expires (Ice House)
 */
enum class ResetType { ROLLOVER, TURNS, TURN_ROLLOVER, AVATAR, NEVER }

/**
 * The 20 most common banishers. Unknown banishers map to [UNKNOWN] which is treated
 * as ROLLOVER so it's safely cleared on next login.
 *
 * Sourced from desktop `BanishManager.Banisher` enum.
 */
enum class Banisher(
    val canonicalName: String,
    val turns: Int,            // -1 means rollover/avatar-only reset
    val resetType: ResetType,
    val isTurnFree: Boolean,
) {
    ANCHOR_BOMB("anchor bomb", 30, ResetType.TURN_ROLLOVER, true),
    BANISHING_SHOUT("banishing shout", -1, ResetType.AVATAR, false),
    BEANCANNON("beancannon", -1, ResetType.ROLLOVER, false),
    BOWL_A_CURVEBALL("Bowl a Curveball", -1, ResetType.ROLLOVER, true),
    BREATHE_OUT("breathe out", 20, ResetType.TURN_ROLLOVER, true),
    CHATTERBOXING("chatterboxing", 20, ResetType.TURN_ROLLOVER, true),
    DIVINE_CHAMPAGNE_POPPER("divine champagne popper", 5, ResetType.TURNS, true),
    FEEL_HATRED("Feel Hatred", 50, ResetType.TURN_ROLLOVER, true),
    ICE_HOUSE("ice house", -1, ResetType.NEVER, false),
    KGB_TRANQUILIZER_DART("KGB tranquilizer dart", 20, ResetType.TURN_ROLLOVER, true),
    LOUDER_THAN_BOMB("louder than bomb", 20, ResetType.TURN_ROLLOVER, true),
    MAFIA_MIDDLEFINGER_RING("mafia middle finger ring", 60, ResetType.TURN_ROLLOVER, true),
    PANTSGIVING("pantsgiving", 30, ResetType.TURN_ROLLOVER, false),
    REFLEX_HAMMER("Reflex Hammer", 30, ResetType.TURN_ROLLOVER, true),
    SABER_FORCE("Saber Force", 30, ResetType.TURN_ROLLOVER, true),
    SNOKEBOMB("snokebomb", 30, ResetType.TURN_ROLLOVER, true),
    SPRING_LOADED_FRONT_BUMPER("Spring-Loaded Front Bumper", 30, ResetType.TURN_ROLLOVER, true),
    STUFFED_YAM_STINKBOMB("stuffed yam stinkbomb", 15, ResetType.TURN_ROLLOVER, true),
    SYSTEM_SWEEP("System Sweep", -1, ResetType.ROLLOVER, false),
    THROW_LATTE_ON_OPPONENT("Throw Latte on Opponent", 30, ResetType.TURN_ROLLOVER, true),
    UNKNOWN("unknown", -1, ResetType.ROLLOVER, false);

    companion object {
        fun fromName(name: String): Banisher =
            entries.firstOrNull { it.canonicalName.equals(name, ignoreCase = true) } ?: UNKNOWN
    }
}
