package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Fight.php HTML → [TrackManager.trackMonster] hooks (Phases 1091–1110 / 1121–1130).
 * Pattern strings sourced from desktop [FightRequest] track branches.
 */
object FightTrackSync {
    private val PATTERNS: List<Pair<String, TrackManager.Tracker>> = listOf(
        "You carefully examine the ground around you" to TrackManager.Tracker.OLFACTION,
        "You sniff deeply" to TrackManager.Tracker.OLFACTION,
        "transcendentally" to TrackManager.Tracker.OLFACTION,
        "Your nose twitches" to TrackManager.Tracker.NOSY_NOSE,
        "Gallapagosian Mating Call" to TrackManager.Tracker.GALLAPAGOS,
        "make friends with your opponent" to TrackManager.Tracker.MAKE_FRIENDS,
        "Long Con" to TrackManager.Tracker.LONG_CON,
        "Perceive Soul" to TrackManager.Tracker.PERCEIVE_SOUL,
        "Motif" to TrackManager.Tracker.MOTIF,
        "Monkey Point" to TrackManager.Tracker.MONKEY_POINT,
        "You hunt your foe" to TrackManager.Tracker.HUNT,
        "Offer Latte to Opponent" to TrackManager.Tracker.LATTE,
        "offer your opponent a latte" to TrackManager.Tracker.LATTE,
        "Be Superficially interested" to TrackManager.Tracker.SUPERFICIAL,
        "Staff of the Cream of the Cream" to TrackManager.Tracker.CREAM_JIGGLE,
        "jiggle the cream" to TrackManager.Tracker.CREAM_JIGGLE,
        "Curse of Stench" to TrackManager.Tracker.CURSE_OF_STENCH,
        "prank Crimbo card" to TrackManager.Tracker.PRANK_CARD,
        "trick coin" to TrackManager.Tracker.TRICK_COIN,
        "McHugeLarge Slash" to TrackManager.Tracker.MCHUGELARGE_SLASH,
        "Meat Cute" to TrackManager.Tracker.MEAT_CUTE,
        "Try to Remember" to TrackManager.Tracker.TRY_TO_REMEMBER,
        "Baseball Diamond" to TrackManager.Tracker.BASEBALL_DIAMOND,
        "Left %n Kick" to TrackManager.Tracker.LEFT_ZOOT_KICK,
        "Right %n Kick" to TrackManager.Tracker.RIGHT_ZOOT_KICK,
        "off into the distance and likely won't return" to TrackManager.Tracker.LEFT_ZOOT_KICK,
    )

    /**
     * If fight HTML matches a known track pattern, records the track for [monsterName].
     * @return the tracker applied, or null if none matched
     */
    fun applyFromFight(
        html: String,
        monsterName: String,
        preferences: Preferences,
        currentTurn: Int,
    ): TrackManager.Tracker? {
        if (monsterName.isBlank() || monsterName.equals("Unknown", ignoreCase = true)) return null
        val tracker = PATTERNS.firstOrNull { (text, _) -> html.contains(text, ignoreCase = true) }?.second
            ?: return null
        TrackManager.trackMonster(preferences, monsterName, tracker, currentTurn)
        return tracker
    }
}
