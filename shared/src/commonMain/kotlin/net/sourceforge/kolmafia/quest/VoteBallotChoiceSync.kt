package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.modifiers.ModifierEnchantmentParser
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.VoteMonsterManager

/**
 * Desktop [ChoiceControl] Daily Loathing Ballot choice 1331.
 * Distinct from town booth [VoteMonsterManager.applyFromVisit].
 */
object VoteBallotChoiceSync {

    const val CHOICE_ID = 1331

    private val VOTE_PATTERN = Regex(
        """value=["'](\d)["']\s+class=["']locals["'][^>]*>.*?<span[^>]*>(.*?)</span>""",
        RegexOption.IGNORE_CASE,
    )
    private val VOTE_SPEECH_PATTERN = Regex(
        """name=["']g["'][^>]*>\s*<b>(.*?)</b>(.*?)<br\s*/?\s*><blockquote>(.*?)</blockquote>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val URL_VOTE_PATTERN = Regex("""local\[\]=(\d)""")

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        for (match in VOTE_PATTERN.findAll(html)) {
            val voteValue = (match.groupValues[1].toIntOrNull() ?: continue) + 1
            val voteMod = ModifierEnchantmentParser.parseModifier(match.groupValues[2])
            if (!voteMod.isNullOrBlank()) {
                preferences.setString("_voteLocal$voteValue", voteMod)
                changed = true
            }
        }
        var count = 1
        for (match in VOTE_SPEECH_PATTERN.findAll(html)) {
            val party = match.groupValues[2]
            val speech = match.groupValues[3]
            val monster = VoteMonsterManager.monsterFromBallotSpeech(party, speech)
            if (monster != null) {
                preferences.setString("_voteMonster$count", monster)
                changed = true
            }
            count++
        }
        return changed
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        if (html.contains("must vote for a candidate")) return false
        val mods = mutableListOf<String>()
        for (match in URL_VOTE_PATTERN.findAll(choiceUrl)) {
            val vote = (match.groupValues[1].toIntOrNull() ?: continue) + 1
            val pref = preferences.getString("_voteLocal$vote", "")
            if (pref.isNotBlank()) {
                mods += pref.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        if (mods.isEmpty()) return false
        preferences.setString("_voteModifier", mods.joinToString(", "))
        sessionLog("You have cast your votes")
        return true
    }
}
