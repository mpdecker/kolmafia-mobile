package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Desktop [ChoiceControl] Play Ball! choice 1598 —
 * innings visit + postChoiceBaseball monster routing.
 */
object BaseballChoiceSync {

    const val CHOICE_ID = 1598

    const val INNINGS_PREF = "_baseballInnings"
    const val MAX_INNINGS = 3

    private val STRIKE_PATTERN = Regex("""<s>(.*?)</s>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("<s>")) return false
        val current = preferences.getInt(INNINGS_PREF, 0)
        preferences.setInt(INNINGS_PREF, (current + 1).coerceAtMost(MAX_INNINGS))
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        currentTurn: Int = 0,
        banishMonster: (String, Banisher, Int) -> Unit = { _, _, _ -> },
        trackMonster: (String, TrackManager.Tracker, Int) -> Unit = { _, _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision == 6) return false // diamond desc refresh — no pref write here
        val out = extractOutputText(html)
        val monster = getBaseballMonster(html) ?: return false
        return when {
            out.contains("Instead of a baseball, you throw a big handful of ice") -> {
                banishMonster(monster, Banisher.BASEBALL_DIAMOND, currentTurn)
                true
            }
            out.contains("You draw a skull on the ball") -> {
                preferences.setString("_skullballMonster", monster)
                true
            }
            out.contains("You throw the baseball using a Forbidden Ratio") -> {
                preferences.setString("_curveballMonster", monster)
                preferences.setInt("_curveballFightsLeft", 3)
                true
            }
            out.contains("You dip a baseball in a big can of expired") -> {
                preferences.setString("_beanballMonster", monster)
                true
            }
            out.contains("You coat the ball in some melted cheddar cheese") -> {
                trackMonster(monster, TrackManager.Tracker.BASEBALL_DIAMOND, currentTurn)
                true
            }
            out.contains("You throw a screwball") -> {
                preferences.setString("_screwballMonster", monster)
                true
            }
            else -> false
        }
    }

    /** Desktop [ChoiceControl.getBaseballMonster] — last `<s>` text with first 3 chars stripped. */
    fun getBaseballMonster(html: String): String? {
        val last = STRIKE_PATTERN.findAll(html).lastOrNull()?.groupValues?.getOrNull(1) ?: return null
        val text = last.replace(Regex("<[^>]+>"), "").trim()
        if (text.length <= 3) return null
        return text.substring(3).trim().takeIf { it.isNotEmpty() }
    }

    private fun extractOutputText(html: String): String {
        val idMatch = Regex(
            """id=["']output["'][^>]*>(.*?)</""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)
        return idMatch?.groupValues?.getOrNull(1)
            ?: html.replace(Regex("<[^>]+>"), " ")
    }

    fun defaultBanish(preferences: Preferences): (String, Banisher, Int) -> Unit = { name, banisher, turn ->
        BanishManager(preferences).banishMonster(name, banisher, turn)
    }

    fun defaultTrack(preferences: Preferences): (String, TrackManager.Tracker, Int) -> Unit =
        { name, tracker, turn ->
            TrackManager.trackMonster(preferences, name, tracker, turn)
        }
}
