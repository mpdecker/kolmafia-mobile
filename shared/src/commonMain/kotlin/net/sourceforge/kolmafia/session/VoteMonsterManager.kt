package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.modifiers.ModifierEnchantmentParser
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest

/**
 * Desktop [VoteMonsterManager] — Vote Monster turn counter, now-check, and booth parse.
 */
object VoteMonsterManager {

    const val COUNTER_LABEL = "Vote Monster"
    const val COUNTER_IMAGE = "absballot.gif"
    const val TRACK_PREF = "trackVoteMonster"
    const val VOTER_REGISTRATION_FORM = "voter registration form"

    private val VOTE_PATTERN =
        Regex("initiatives: </b><div style='margin-left: 1em; color: blue'>(.*?)<br>(.*?)</div>")
    private val VOTE_SPEECH_PATTERN =
        Regex("<b>Today's Leader: </b>(.*?)<br><blockquote>(.*?)</blockquote>")

    fun checkCounter(
        preferences: Preferences?,
        turnsPlayed: Int,
        isAllowed: (RestrictedItemType, String) -> Boolean = { type, key ->
            StandardRequest.isAllowed(type, key, null)
        },
    ): Boolean {
        val prefs = preferences ?: return false
        if (!isAllowed(RestrictedItemType.ITEMS, VOTER_REGISTRATION_FORM)) return false
        if (prefs.getString(TRACK_PREF) == "false") return false
        if (prefs.getString(TRACK_PREF) == "free" && prefs.getInt("_voteFreeFights", 0) >= 3) {
            return false
        }
        if (TurnCounter.isCounting(prefs, COUNTER_LABEL, turnsPlayed)) return false
        val turns = 11 - ((turnsPlayed - 1) % 11)
        TurnCounter.startCounting(prefs, turnsPlayed, turns, COUNTER_LABEL, COUNTER_IMAGE)
        return true
    }

    fun voteMonsterNow(turnsPlayed: Int, lastVoteMonsterTurn: Int): Boolean =
        turnsPlayed % 11 == 1 && lastVoteMonsterTurn != turnsPlayed

    fun voteMonsterNow(turnsPlayed: Int, preferences: Preferences?): Boolean =
        voteMonsterNow(turnsPlayed, preferences?.getInt("lastVoteMonsterTurn", 0) ?: 0)

    fun applyFromVisit(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("action=townright_vote", ignoreCase = true)) return false
        return parseBooth(html, preferences)
    }

    fun parseBooth(html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        if (!html.contains("Today's Leader")) return false
        var changed = false
        val voteMatch = VOTE_PATTERN.find(html)
        if (voteMatch != null) {
            val mods = listOfNotNull(
                ModifierEnchantmentParser.parseModifier(voteMatch.groupValues[1]),
                ModifierEnchantmentParser.parseModifier(voteMatch.groupValues[2]),
            ).filter { it.isNotBlank() }
            if (mods.isNotEmpty()) {
                prefs.setString("_voteModifier", mods.joinToString(", "))
                changed = true
            }
        }
        if (prefs.getString("_voteMonster").isEmpty()) {
            val speechMatch = VOTE_SPEECH_PATTERN.find(html)
            if (speechMatch != null) {
                val monster = monsterFromSpeech(speechMatch.groupValues[1], speechMatch.groupValues[2])
                if (monster != null) {
                    prefs.setString("_voteMonster", monster)
                    changed = true
                }
            }
        }
        return changed
    }

    fun monsterFromBallotSpeech(party: String, speech: String): String? =
        monsterFromSpeech(party, speech)

    private fun monsterFromSpeech(party: String, speech: String): String? = when {
        party.contains("Pork Elf Historical Preservation Party") -> when {
            speech.contains("strict curtailing of unnatural modern technologies") ->
                "government bureaucrat"
            speech.contains("reintroduce Pork Elf DNA") -> "terrible mutant"
            speech.contains("kingdom-wide seance") -> "angry ghost"
            speech.contains("very interested in snakes") -> "annoyed snake"
            speech.contains("lots of magical lard") -> "slime blob"
            else -> null
        }
        party.contains("Clan Ventrilo") -> when {
            speech.contains("bringing this blessing to the entire population") -> "slime blob"
            speech.contains("see your deceased loved ones again") -> "angry ghost"
            speech.contains("stronger and more vigorous") -> "terrible mutant"
            speech.contains("implement healthcare reforms") -> "government bureaucrat"
            speech.contains("flavored drink in a tube") -> "annoyed snake"
            else -> null
        }
        party.contains("Bureau of Efficient Government") -> when {
            speech.contains("graveyards are a terribly inefficient use of space") -> "angry ghost"
            speech.contains("strictly enforced efficiency laws") -> "government bureaucrat"
            speech.contains("distribute all the medications for all known diseases ") ->
                "terrible mutant"
            speech.contains("introduce an influx of snakes") -> "annoyed snake"
            speech.contains("releasing ambulatory garbage-eating slimes") -> "slime blob"
            else -> null
        }
        party.contains("Scions of Ich'Xuul'kor") -> when {
            speech.contains("increase awareness of our really great god") -> "terrible mutant"
            speech.contains("hunt these evil people down") -> "government bureaucrat"
            speech.contains("sound of a great hissing") -> "annoyed snake"
            speech.contains("make things a little bit more like he's used to") -> "slime blob"
            speech.contains("kindness energy") -> "angry ghost"
            else -> null
        }
        party.contains("Extra-Terrific Party") -> when {
            speech.contains("wondrous chemical") -> "terrible mutant"
            speech.contains("comprehensive DNA harvesting program") -> "government bureaucrat"
            speech.contains("mining and refining processes begin") -> "slime blob"
            speech.contains("warp engines will not destabilize") -> "angry ghost"
            speech.contains("breeding pair of these delightful creatures") -> "annoyed snake"
            else -> null
        }
        else -> null
    }
}
