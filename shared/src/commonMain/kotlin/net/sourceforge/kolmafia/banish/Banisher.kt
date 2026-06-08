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
 * The 69 known banishers. Unknown banishers map to [UNKNOWN] which is treated
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
    BALEFUL_HOWL("baleful howl", -1, ResetType.ROLLOVER, true),
    BANISHING_SHOUT("banishing shout", -1, ResetType.AVATAR, false),
    BASEBALL_DIAMOND("Baseball Diamond", -1, ResetType.ROLLOVER, true),
    BATTER_UP("batter up!", -1, ResetType.ROLLOVER, false),
    BE_A_MIND_MASTER("Be a Mind Master", 80, ResetType.TURNS, true),
    BEANCANNON("beancannon", -1, ResetType.ROLLOVER, false),
    BLART_SPRAY_WIDE("B. L. A. R. T. Spray (wide)", -1, ResetType.ROLLOVER, true),
    BOWL_A_CURVEBALL("Bowl a Curveball", -1, ResetType.ROLLOVER, true),
    BREATHE_OUT("breathe out", 20, ResetType.TURN_ROLLOVER, true),
    BUNDLE_OF_FRAGRANT_HERBS("bundle of &quot;fragrant&quot; herbs", -1, ResetType.ROLLOVER, true),
    CHATTERBOXING("chatterboxing", 20, ResetType.TURN_ROLLOVER, true),
    CLASSY_MONKEY("classy monkey", 20, ResetType.TURNS, false),
    COCKTAIL_NAPKIN("cocktail napkin", 20, ResetType.TURNS, true),
    CRIMBUCCANEER_RIGGING_LASSO("Crimbuccaneer rigging lasso", 100, ResetType.TURN_ROLLOVER, false),
    CRYSTAL_SKULL("crystal skull", 20, ResetType.TURNS, false),
    CURSE_OF_VACATION("curse of vacation", -1, ResetType.ROLLOVER, false),
    DEATHCHUCKS("deathchucks", -1, ResetType.ROLLOVER, true),
    DIRTY_STINKBOMB("dirty stinkbomb", -1, ResetType.ROLLOVER, true),
    DIVINE_CHAMPAGNE_POPPER("divine champagne popper", 5, ResetType.TURNS, true),
    FEEL_HATRED("Feel Hatred", 50, ResetType.TURN_ROLLOVER, true),
    GINGERBREAD_RESTRAINING_ORDER("gingerbread restraining order", -1, ResetType.ROLLOVER, false),
    GLITCHED_MALWARE("Deploy Glitched Malware", -1, ResetType.ROLLOVER, false),
    HAROLDS_BELL("harold's bell", 20, ResetType.TURNS, false),
    HEARTSTONE_BANISH("Heartstone %banish", 50, ResetType.TURNS, false),
    HOWL_OF_THE_ALPHA("howl of the alpha", -1, ResetType.AVATAR, false),
    HUMAN_MUSK("human musk", -1, ResetType.ROLLOVER, true),
    ICE_HOTEL_BELL("ice hotel bell", -1, ResetType.ROLLOVER, true),
    ICE_HOUSE("ice house", -1, ResetType.NEVER, false),
    KGB_TRANQUILIZER_DART("KGB tranquilizer dart", 20, ResetType.TURN_ROLLOVER, true),
    LEFT_ZOOT_KICK("Left %n Kick", 100, ResetType.TURNS, true),
    LICORICE_ROPE("licorice rope", -1, ResetType.ROLLOVER, false),
    LOUDER_THAN_BOMB("louder than bomb", 20, ResetType.TURN_ROLLOVER, true),
    MAFIA_MIDDLEFINGER_RING("mafia middle finger ring", 60, ResetType.TURN_ROLLOVER, true),
    MARK_YOUR_TERRITORY("Mark Your Territory", -1, ResetType.ROLLOVER, false),
    MONKEY_SLAP("Monkey Slap", -1, ResetType.ROLLOVER, false),
    NANORHINO("nanorhino", -1, ResetType.ROLLOVER, false),
    PANTSGIVING("pantsgiving", 30, ResetType.TURN_ROLLOVER, false),
    PATRIOTIC_SCREECH("Patriotic Screech", 100, ResetType.TURNS, false),
    PEEL_OUT("peel out", -1, ResetType.AVATAR, true),
    PEPPERMINT_BOMB("peppermint bomb", 100, ResetType.TURN_ROLLOVER, false),
    PULLED_INDIGO_TAFFY("pulled indigo taffy", 40, ResetType.TURNS, true),
    PUNCH_OUT_YOUR_FOE("Punch Out your Foe", 20, ResetType.TURNS, true),
    PUNT_AOSOL("[28021]Punt", -1, ResetType.ROLLOVER, false),
    PUNT_WEREPROF("[7510]Punt", 40, ResetType.TURNS, false),
    REFLEX_HAMMER("Reflex Hammer", 30, ResetType.TURN_ROLLOVER, true),
    RIGHT_ZOOT_KICK("Right %n Kick", 100, ResetType.TURNS, true),
    ROAR_LIKE_A_LION("Roar like a Lion", -1, ResetType.ROLLOVER, false),
    SABER_FORCE("Saber Force", 30, ResetType.TURN_ROLLOVER, true),
    SEADENT_LIGHTNING("Sea *dent", -1, ResetType.ROLLOVER, false),
    SHOW_YOUR_BORING_FAMILIAR_PICTURES("Show your boring familiar pictures", 100, ResetType.TURNS, true),
    SMOKE_GRENADE("smoke grenade", 20, ResetType.TURNS, false),
    SNOKEBOMB("snokebomb", 30, ResetType.TURN_ROLLOVER, true),
    SPOOKY_MUSIC_BOX_MECHANISM("spooky music box mechanism", -1, ResetType.ROLLOVER, false),
    SPLIT_PEA_SOUP("handful of split pea soup", 30, ResetType.TURN_ROLLOVER, true),
    SPRING_KICK("Spring Kick", -1, ResetType.ROLLOVER, true),
    SPRING_LOADED_FRONT_BUMPER("Spring-Loaded Front Bumper", 30, ResetType.TURN_ROLLOVER, true),
    STAFF_OF_THE_STANDALONE_CHEESE("staff of the standalone cheese", -1, ResetType.AVATAR, false),
    STINKY_CHEESE_EYE("stinky cheese eye", 10, ResetType.TURNS, true),
    STUFFED_YAM_STINKBOMB("stuffed yam stinkbomb", 15, ResetType.TURN_ROLLOVER, true),
    SYSTEM_SWEEP("System Sweep", -1, ResetType.ROLLOVER, false),
    TENNIS_BALL("tennis ball", 30, ResetType.TURN_ROLLOVER, true),
    THROWIN_EMBER("throwin' ember", 30, ResetType.TURN_ROLLOVER, false),
    THROW_LATTE_ON_OPPONENT("Throw Latte on Opponent", 30, ResetType.TURN_ROLLOVER, true),
    THUNDER_CLAP("thunder clap", 40, ResetType.TURNS, false),
    TRYPTOPHAN_DART("tryptophan dart", -1, ResetType.ROLLOVER, false),
    ULTRA_HAMMER("Ultra Hammer", -1, ResetType.ROLLOVER, false),
    V_FOR_VIVALA_MASK("v for vivala mask", 10, ResetType.TURNS, true),
    WALK_AWAY_FROM_EXPLOSION("walk away from explosion", 30, ResetType.TURNS, false),
    UNKNOWN("unknown", -1, ResetType.ROLLOVER, false);

    companion object {
        fun fromName(name: String): Banisher =
            entries.firstOrNull { it.canonicalName.equals(name, ignoreCase = true) } ?: UNKNOWN
    }
}
