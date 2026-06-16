package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses Naughty Sorceress contest booth and hedge-maze trap choice text.
 * Mirrors desktop [SorceressLairManager.parseContestBooth] and [parseMazeTrap].
 */
object ContestBoothSync {

    private val rankPattern = Regex("""<b>#(\d+)</b>""")
    private val optimismPattern = Regex(
        """You feel (.*?) about your chances in the (.*?) Adventurer contest""",
    )
    private val enteredPattern = Regex(
        """&quot;You already entered the (.*?) Adventurer contest.*?&quot;""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val queuedPattern = Regex("""there are (\d+) (Adventurers|other Adventurers|of them)""")

    fun parseContestBooth(
        decision: Int,
        responseText: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (preferences == null) return false
        var changed = false

        if (decision == 4) {
            if (responseText.contains("World's Best Adventurer sash")) {
                changed = advanceQuest(questDatabase, Quest.FINAL, "step3") || changed
            }
            return changed
        }

        changed = advanceQuest(questDatabase, Quest.FINAL, QuestDatabase.STARTED) || changed

        if (decision in 1..3) {
            val rank = rankPattern.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
            if (rank != null) {
                preferences.setInt("nsContestants$decision", rank - 1)
                changed = true
            }
            return changed
        }

        for (match in optimismPattern.findAll(responseText)) {
            val contest = match.groupValues[2]
            if (contest == "Fastest") continue
            contestToStat(contest)?.let {
                preferences.setString("nsChallenge1", it)
                changed = true
            }
            contestToElement(contest)?.let {
                preferences.setString("nsChallenge2", it)
                changed = true
            }
        }

        for (match in enteredPattern.findAll(responseText)) {
            val contest = match.groupValues[1]
            val text = match.value
            val queue = queuedPattern.find(text)?.groupValues?.get(1)?.toIntOrNull()
                ?: when {
                    text.contains("you and one other") ||
                        text.contains("only one other Adventurer besides you") ||
                        text.contains("only one other person in that contest") -> 1
                    text.contains("only Adventurer") || text.contains("only entrant") -> 0
                    else -> -1
                }

            if (contest == "Fastest") {
                preferences.setInt("nsContestants1", queue)
                changed = true
                continue
            }
            contestToStat(contest)?.let {
                preferences.setInt("nsContestants2", queue)
                preferences.setString("nsChallenge1", it)
                changed = true
            }
            contestToElement(contest)?.let {
                preferences.setInt("nsContestants3", queue)
                preferences.setString("nsChallenge2", it)
                changed = true
            }
        }
        return changed
    }

    fun parseMazeTrap(choice: Int, responseText: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        val setting = when (choice) {
            1005 -> "nsChallenge3"
            1008 -> "nsChallenge4"
            1011 -> "nsChallenge5"
            else -> return false
        }
        val element = when {
            responseText.contains("hot damage") -> "hot"
            responseText.contains("cold damage") -> "cold"
            responseText.contains("spooky damage") -> "spooky"
            responseText.contains("stench damage") -> "stench"
            responseText.contains("sleaze damage") -> "sleaze"
            else -> return false
        }
        preferences.setString(setting, element)
        return true
    }

    fun visitHedgeMazeChoice(choice: Int, preferences: Preferences?, questDatabase: QuestDatabase?): Boolean {
        if (choice !in 1005..1013) return false
        preferences?.setInt("currentHedgeMazeRoom", choice - 1004)
        return advanceQuest(questDatabase, Quest.FINAL, "step4")
    }

    private fun contestToStat(contest: String): String? = when (contest) {
        "Strongest" -> "Muscle"
        "Smartest" -> "Mysticality"
        "Smoothest" -> "Moxie"
        else -> null
    }

    private fun contestToElement(contest: String): String? = when (contest) {
        "Hottest" -> "hot"
        "Coldest" -> "cold"
        "Spookiest" -> "spooky"
        "Stinkiest" -> "stench"
        "Sleaziest" -> "sleaze"
        else -> null
    }

    private fun advanceQuest(questDatabase: QuestDatabase?, quest: Quest, step: String): Boolean {
        if (questDatabase == null) return false
        val current = questDatabase.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) <= QuestDatabase.stepOrdinal(current)) return false
        questDatabase.setProgress(quest, step)
        return true
    }
}
