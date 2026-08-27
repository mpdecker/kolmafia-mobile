package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest.getRound] / [FightRequest.logText] scaffolding (Phases 1611–1625)
 * plus attribute / special-damage Round lines (Phases 1656–1670).
 */
object FightSessionLog {

    private val ATTR_GAIN_LOSE = Regex(
        """You (?:gain|lose) ([\d,]+) (?:hit points?|H\.?P\.?|Mana Points?|M\.?P\.?|""" +
            """Muscle|Mysticality|Moxie|Fortitude|Wizardliness|Cheek|""" +
            """Beefiness|Enchantedness|Chutzpah|Strength|Magicalness|Roguishness|""" +
            """Power|Smarm)""",
        RegexOption.IGNORE_CASE,
    )

    private val SPECIAL_DAMAGE_PHRASES = listOf(
        "continues to bleed",
        "from the poison",
        "wave of toddlers",
        "second wave follows",
        "third group arrives",
        "Bone Homie floats",
        "Your stomach gurgles",
        "Your insides vibrate",
        "Your pygmy buddy staggers",
        "disgusting coating of mayonnaise",
        "EVISCERATE",
        "sound of distant thunder",
        "horrible stench of your armpits",
        "burning beard hair",
        "belch flames at your foe",
        "damage from the demonic fire",
        "Your dalmatian bites your opponent",
        "burns you from head to toe",
    )

    /**
     * Desktop [FightRequest.getRound] — Round N: when mid-fight, After Battle: when round is 0.
     */
    fun roundPrefix(currentRound: Int = ChoiceCombatAshState.currentRound): String =
        if (currentRound <= 0) "After Battle: " else "Round $currentRound: "

    fun logText(
        message: String,
        sessionLogger: SessionLogger?,
        currentRound: Int = ChoiceCombatAshState.currentRound,
    ) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || sessionLogger == null) return
        sessionLogger.appendRawLine(roundPrefix(currentRound) + trimmed)
    }

    /** High-traffic gain/lose attribute lines → Round-prefixed session log. */
    fun logPlayerAttributes(
        html: String,
        sessionLogger: SessionLogger?,
        currentRound: Int = ChoiceCombatAshState.currentRound,
    ): Boolean {
        if (sessionLogger == null || html.isBlank()) return false
        var logged = false
        for (m in ATTR_GAIN_LOSE.findAll(html.replace(Regex("<[^>]+>"), " "))) {
            val line = m.value.trim().trimEnd('.')
            if (line.isNotEmpty()) {
                logText(line, sessionLogger, currentRound)
                logged = true
            }
        }
        return logged
    }

    fun logSpecialDamage(
        html: String,
        sessionLogger: SessionLogger?,
        currentRound: Int = ChoiceCombatAshState.currentRound,
    ): Boolean {
        if (sessionLogger == null || html.isBlank()) return false
        val plain = html.replace(Regex("<[^>]+>"), " ")
        var logged = false
        for (phrase in SPECIAL_DAMAGE_PHRASES) {
            if (plain.contains(phrase)) {
                // Log a short excerpt around the phrase
                val idx = plain.indexOf(phrase)
                val start = (idx - 40).coerceAtLeast(0)
                val end = (idx + phrase.length + 80).coerceAtMost(plain.length)
                val excerpt = plain.substring(start, end).replace(Regex("\\s+"), " ").trim()
                logText(excerpt, sessionLogger, currentRound)
                logged = true
            }
        }
        return logged
    }

    fun logWin(
        monsterName: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences? = null,
    ) {
        val name = monsterName.ifBlank {
            preferences?.getString(Preferences.LAST_MONSTER, "").orEmpty()
                .ifBlank { MonsterStatusTracker.getLastMonsterName() }
        }
        if (name.isNotBlank()) {
            logText("$name wins the fight!", sessionLogger, currentRound = 0)
        }
    }

    /**
     * Apply Round/After Battle scaffolding for a fight response.
     * Call after [ChoiceCombatAshState.noteFightRound] so [currentRound] is current.
     */
    fun apply(
        html: String,
        sessionLogger: SessionLogger?,
        won: Boolean = false,
        fightEnded: Boolean = false,
        monsterName: String = "",
        preferences: Preferences? = null,
        logMonsterHealth: Boolean = preferences?.getBoolean("logMonsterHealth", false) == true,
    ): Boolean {
        if (sessionLogger == null || html.isBlank()) return false
        var changed = false
        val round = if (fightEnded) 0 else ChoiceCombatAshState.currentRound
        changed = logPlayerAttributes(html, sessionLogger, round) || changed
        changed = logSpecialDamage(html, sessionLogger, round) || changed
        if (logMonsterHealth) {
            changed = logHealthEstimates(html, sessionLogger, round) || changed
        }
        if (won && fightEnded) {
            logWin(monsterName, sessionLogger, preferences)
            changed = true
        }
        return changed
    }

    private fun logHealthEstimates(
        html: String,
        sessionLogger: SessionLogger,
        currentRound: Int,
    ): Boolean {
        var logged = false
        val monster = MonsterStatusTracker.getLastMonsterName().ifBlank { "monster" }
        Regex(
            """I deduce that this monster has approximately (\d+) hit points""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.getOrNull(1)?.let { hp ->
            logText(
                "$monster shows detective skull health estimate of $hp",
                sessionLogger,
                currentRound,
            )
            logged = true
        }
        Regex(
            """toy space helmet.*?(\d+)""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.getOrNull(1)?.let { hp ->
            logText(
                "$monster shows toy space helmet health estimate of $hp",
                sessionLogger,
                currentRound,
            )
            logged = true
        }
        return logged
    }
}
