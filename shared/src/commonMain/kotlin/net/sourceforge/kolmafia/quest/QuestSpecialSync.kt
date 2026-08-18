package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Quest-log detail handlers for quests with multi-variant step text (Telegram, Party Fair, Oracle).
 * Mirrors desktop QuestDatabase special-case parsers.
 */
object QuestSpecialSync {

    private data class TelegramStep(
        val signal: String,
        val questName: String,
        val difficulty: Int,
        val step: String,
    )

    private val telegramSteps = listOf(
        TelegramStep("Ask around the Rough Diamond Saloon", "Missing: Fancy Man", 1, "step1"),
        TelegramStep("Trek across the desert to Jeff's mining claim", "Missing: Fancy Man", 1, "step2"),
        TelegramStep("Delve deeper into Jeff's Fancy Mine", "Missing: Fancy Man", 1, "step3"),
        TelegramStep("Defeat Jeff the Fancy Skeleton", "Missing: Fancy Man", 1, "step4"),
        TelegramStep("Search for Daisy's homestead", "Missing: Pioneer Daughter", 1, "step1"),
        TelegramStep("Question the cultists in Bloodmilk Cave", "Missing: Pioneer Daughter", 1, "step2"),
        TelegramStep("Fight your way through Daisy's Fortress", "Missing: Pioneer Daughter", 1, "step3"),
        TelegramStep("Defeat Daisy the Unclean", "Missing: Pioneer Daughter", 1, "step4"),
        TelegramStep("Clear some of the criminals out of Spitback", "Help!  Desperados!", 1, "step1"),
        TelegramStep("Find your way to Pecos Dave's hideout", "Help!  Desperados!", 1, "step2"),
        TelegramStep("Find Pecos Dave in his mine hideout", "Help!  Desperados!", 1, "step3"),
        TelegramStep("Defeat Pecos Dave", "Help!  Desperados!", 1, "step4"),
        TelegramStep("Find the pastor in his church", "Haunted Boneyard", 2, "step1"),
        TelegramStep("Investigate the local cemetery", "Haunted Boneyard", 2, "step2"),
        TelegramStep("Clear out the ancient cow burial ground", "Haunted Boneyard", 2, "step3"),
        TelegramStep("Defeat Amoon-Ra Cowtep", "Haunted Boneyard", 2, "step4"),
        TelegramStep("Fight your way through the crowd at the gambling tournament", "Big Gambling Tournament Announced", 2, "step1"),
        TelegramStep("Escape from the snake pit", "Big Gambling Tournament Announced", 2, "step2"),
        TelegramStep("Track down Snakeye Glenn at the Great Western hotel", "Big Gambling Tournament Announced", 2, "step3"),
        TelegramStep("Defeat Snakeeye Glenn", "Big Gambling Tournament Announced", 2, "step4"),
        TelegramStep("Fight your way to the sheriff's office and apply for the job", "Sheriff Wanted", 2, "step1"),
        TelegramStep("Head up river to the Placid Lake Gang's hideout", "Sheriff Wanted", 2, "step2"),
        TelegramStep("Search the hideout for the gang's leader", "Sheriff Wanted", 2, "step3"),
        TelegramStep("Defeat Former Sheriff Dan Driscoll", "Sheriff Wanted", 2, "step4"),
        TelegramStep("Figure out what's going wrong at the mine", "Madness at the Mine", 3, "step1"),
        TelegramStep("Search the desert for the missing foreman", "Madness at the Mine", 3, "step2"),
        TelegramStep("Find that door in the mine again", "Madness at the Mine", 3, "step3"),
        TelegramStep("Defeat the unusual construct", "Madness at the Mine", 3, "step4"),
        TelegramStep("Find out why the children are going missing", "Missing: Many Children", 3, "step1"),
        TelegramStep("Ride the ghost train", "Missing: Many Children", 3, "step2"),
        TelegramStep("Search Cowtown for the missing children", "Missing: Many Children", 3, "step3"),
        TelegramStep("Defeat Clara", "Missing: Many Children", 3, "step4"),
        TelegramStep("Escort the Hackleton wagon train across the desert", "Wagon Train Escort Wanted", 3, "step1"),
        TelegramStep("Defend the Hackleton wagon train", "Wagon Train Escort Wanted", 3, "step2"),
        TelegramStep("Defeat the Hackletons", "Wagon Train Escort Wanted", 3, "step3"),
        TelegramStep("Defeat Granny Hackleton", "Wagon Train Escort Wanted", 3, "step4"),
    )

    private data class PartyFairStep(
        val signal: String,
        val subQuest: String,
        val step: String,
        val progress: String? = null,
    )

    private val partyFairSteps = listOf(
        PartyFairStep("Clean up the trash", "trash", "step1"),
        PartyFairStep("Check the backyard", "booze", QuestDatabase.STARTED, ""),
        PartyFairStep("Gerald at the", "booze", "step1"),
        PartyFairStep("to the backyard of the", "booze", "step2"),
        PartyFairStep("Hype level", "woots", "step1"),
        PartyFairStep("Clear all of the guests", "partiers", "step1"),
        PartyFairStep("see what kind of snacks Geraldine wants", "food", QuestDatabase.STARTED, ""),
        PartyFairStep("for Geraldine at the", "food", "step1"),
        PartyFairStep("to Geraldine in the kitchen", "food", "step2"),
        PartyFairStep("Meat for the DJ", "dj", "step1"),
        PartyFairStep("Remaining bill", "meat", "step1"),
        PartyFairStep("Return to the", "woots", "step2"),
    )

    private val hippyFratPattern = Regex("""Remaining soldiers: (\d+) hippies, +(\d+) frat boys\.""")

    private val oracleTargetPattern = Regex("<b>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)
    private val ghostTargetPattern = Regex("<b>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)
    private val newYouPattern = Regex(
        """Looks like you've cast (.*?) during (\d+) of the required (\d+) encounters with (?:a|an|the|some) (.*?)!""",
    )
    private val shenPattern = Regex("""Recover (.*?) from""")
    private val shen2Pattern = Regex("""Take (.*?) back""")
    private val doctorBagItemPattern = Regex("""Acquire (?:a|an) (.*?)\.""")
    private val doctorBagLocationPattern = Regex(
        """Take (?:a|an) (.*?) to the patient in <a(?:.*?)><b>(.*?)</b></a>\.""",
    )
    internal val partyFairTrashPattern = Regex("""Trash left: ~(.*?) pieces""")
    internal val partyFairWootsPattern = Regex("""Hype level: (\d+) / 100 megawoots""")
    internal val partyFairPartiersPattern = Regex("""Partiers remaining: (\d+)""")
    internal val partyFairMeatPattern = Regex("""Remaining bill: (.*?) Meat""")
    internal val partyFairDjMeatPattern =
        Regex("""collect (.*?) Meat for the DJ""", RegexOption.IGNORE_CASE)
    internal val partyFairCombatTrashPattern =
        Regex("""you clean up (\d+) """, RegexOption.IGNORE_CASE)
    private val partyFairBoozePattern1 = Regex("""Get (\d+) (.*?) for Gerald""")
    private val partyFairBoozePattern2 = Regex("""Take the (\d+) (.*?) to the backyard""")
    private val partyFairFoodPattern1 = Regex("""Get (\d+) (.*?) for Geraldine""")
    private val partyFairFoodPattern2 = Regex("""Take the (\d+) (.*?) to Geraldine""")
    private val booPeakPattern = Regex("""It is currently (\d+)%""")
    private val oilPeakPattern = Regex("""The pressure is currently ([\d.]+) microbowies""")
    private val guzzlrBoozePattern = Regex("""Acquire (?:a|an) (.*?) for your Guzzlr client\.""")
    private val guzzlrLocationPattern = Regex("""Deliver the (.*?) to your Guzzlr client: (.*?) in (.*)\.""")
    private val competitionPattern = Regex("""Contest #(\d+): ((\d+) competitor|(Won!))""")
    private val cyrusAdjectivePattern = Regex("""You remember inadvertently making him ([^.]*?)\.""")

    fun apply(
        responseText: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase? = null,
    ): Boolean {
        var advanced = false
        if (applyTelegram(responseText, questDatabase, preferences)) advanced = true
        if (applyPartyFair(responseText, questDatabase, preferences, gameDatabase)) advanced = true
        if (applyOracle(responseText, questDatabase, preferences)) advanced = true
        if (applyGhost(responseText, questDatabase, preferences)) advanced = true
        if (applyNewYou(responseText, questDatabase, preferences)) advanced = true
        if (applyShen(responseText, questDatabase, preferences)) advanced = true
        if (applyDoctorBag(responseText, questDatabase, preferences)) advanced = true
        if (applyGuzzlr(responseText, questDatabase, preferences, gameDatabase)) advanced = true
        if (applyRufus(responseText, questDatabase, preferences, gameDatabase)) advanced = true
        if (applyPrimordial(responseText, questDatabase, preferences)) advanced = true
        if (applyCompetitionStatus(responseText, questDatabase, preferences)) advanced = true
        if (applyFinalQuestLog(responseText, questDatabase)) advanced = true
        if (applyPeakStatus(responseText, questDatabase, preferences)) advanced = true
        if (applyHippyFratStatus(responseText, questDatabase, preferences)) advanced = true
        return advanced
    }

    internal fun parseTelegramSection(bodyHtml: String, preferences: Preferences?): String? {
        for (step in telegramSteps) {
            if (!bodyHtml.contains(step.signal, ignoreCase = true)) continue
            preferences?.setString("lttQuestName", step.questName)
            preferences?.setInt("lttQuestDifficulty", step.difficulty)
            return step.step
        }
        return null
    }

    internal fun parsePartyFairSection(
        bodyHtml: String,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): String? {
        for (step in partyFairSteps) {
            if (!bodyHtml.contains(step.signal, ignoreCase = true)) continue
            preferences?.setString("_questPartyFairQuest", step.subQuest)
            val progress = resolvePartyFairProgress(bodyHtml, step, gameDatabase)
            if (progress != null) {
                preferences?.setString("_questPartyFairProgress", progress)
            }
            return step.step
        }
        return null
    }

    internal fun parseDoctorBagSection(bodyHtml: String, preferences: Preferences?): String? {
        if (bodyHtml.contains("Acquire ", ignoreCase = true)) {
            val match = doctorBagItemPattern.find(bodyHtml) ?: return null
            preferences?.setString("doctorBagQuestItem", match.groupValues[1].trim())
            return QuestDatabase.STARTED
        }
        if (bodyHtml.contains("to the patient", ignoreCase = true)) {
            val match = doctorBagLocationPattern.find(bodyHtml) ?: return null
            preferences?.setString("doctorBagQuestItem", match.groupValues[1].trim())
            preferences?.setString("doctorBagQuestLocation", match.groupValues[2].trim())
            return "step1"
        }
        return null
    }

    internal fun parseGuzzlrSection(
        bodyHtml: String,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): String? {
        if (bodyHtml.contains("Craft a personalized Guzzlr cocktail.", ignoreCase = true)) {
            preferences?.setString("guzzlrQuestTier", "platinum")
            return QuestDatabase.STARTED
        }
        if (bodyHtml.contains("to your Guzzlr client", ignoreCase = true)) {
            val match = guzzlrLocationPattern.find(bodyHtml) ?: return null
            val booze = match.groupValues[1].trim()
            val boozeId = gameDatabase?.item(booze)?.id ?: 0
            if (boozeId in GUZZLR_PLATINUM_ITEM_IDS ||
                booze.contains("Guzzlr cocktail", ignoreCase = true)
            ) {
                preferences?.setString("guzzlrQuestTier", "platinum")
                preferences?.setString("guzzlrQuestBooze", "Guzzlr cocktail set")
            } else {
                preferences?.setString("guzzlrQuestBooze", booze)
            }
            preferences?.setString("guzzlrQuestClient", match.groupValues[2].trim())
            preferences?.setString("guzzlrQuestLocation", match.groupValues[3].trim())
            return "step1"
        }
        if (bodyHtml.contains("Acquire ", ignoreCase = true) &&
            bodyHtml.contains("Guzzlr client", ignoreCase = true)
        ) {
            val match = guzzlrBoozePattern.find(bodyHtml) ?: return null
            preferences?.setString("guzzlrQuestBooze", match.groupValues[1].trim())
            return QuestDatabase.STARTED
        }
        return null
    }

    internal fun parseRufusSection(
        bodyHtml: String,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): String? {
        if (!bodyHtml.contains("Rufus wants you", ignoreCase = true) &&
            !bodyHtml.contains("Call Rufus", ignoreCase = true)
        ) {
            return null
        }
        val prefs = preferences ?: return null
        return RufusManager(prefs).handleQuestLog(bodyHtml, gameDatabase)
    }

    internal fun parsePeakSection(bodyHtml: String, preferences: Preferences?): String? {
        var updated = false
        when {
            bodyHtml.contains("lit the fire on A-Boo Peak", ignoreCase = true) -> {
                preferences?.setInt("booPeakProgress", 0)
                preferences?.setBoolean("booPeakLit", true)
                updated = true
            }
            bodyHtml.contains("check out A-Boo Peak", ignoreCase = true) -> {
                preferences?.setInt("booPeakProgress", 100)
                updated = true
            }
            else -> booPeakPattern.find(bodyHtml)?.let { match ->
                preferences?.setInt("booPeakProgress", match.groupValues[1].toIntOrNull() ?: 0)
                updated = true
            }
        }
        if (bodyHtml.contains("lit the fire on Twin Peak", ignoreCase = true)) {
            preferences?.setInt("twinPeakProgress", 15)
            updated = true
        }
        when {
            bodyHtml.contains("lit the fire on Oil Peak", ignoreCase = true) -> {
                preferences?.setBoolean("oilPeakLit", true)
                preferences?.setString("oilPeakProgress", "0")
                updated = true
            }
            bodyHtml.contains("go to Oil Peak and investigate", ignoreCase = true) -> {
                preferences?.setString("oilPeakProgress", "310.66")
                updated = true
            }
            else -> oilPeakPattern.find(bodyHtml)?.let { match ->
                preferences?.setString("oilPeakProgress", match.groupValues[1])
                updated = true
            }
        }
        if (!updated) return null
        val peaksComplete = preferences?.getBoolean("booPeakLit") == true &&
            preferences.getInt("twinPeakProgress") == 15 &&
            preferences.getBoolean("oilPeakLit") == true
        return if (peaksComplete) "step3" else "step2"
    }

    internal fun parsePrimordialSection(bodyHtml: String, preferences: Preferences?): String? {
        var step: String? = null
        when {
            bodyHtml.contains("creating an unstoppable supervirus", ignoreCase = true) ->
                step = QuestDatabase.FINISHED
            bodyHtml.contains(
                "finding your way to a higher, warmer, oranger part of the Primordial Soup",
                ignoreCase = true,
            ) -> step = "step1"
            bodyHtml.contains("ran into a virus named Cyrus", ignoreCase = true) ->
                step = "step2"
            bodyHtml.contains("floating aimlessly in the Primordial Soup", ignoreCase = true) ->
                step = QuestDatabase.STARTED
        }
        cyrusAdjectivePattern.find(bodyHtml)?.let { match ->
            val adjectives = match.groupValues[1].replace(" and", ",").split(", ")
            for (adjective in adjectives) {
                appendCyrusAdjective(preferences, adjective.trim())
            }
        }
        return step ?: if (cyrusAdjectivePattern.containsMatchIn(bodyHtml)) QuestDatabase.STARTED else null
    }

    internal fun parseFinalSection(bodyHtml: String): String? {
        for ((signal, step) in finalQuestLogSteps) {
            if (bodyHtml.contains(signal, ignoreCase = true)) return step
        }
        return null
    }

    internal fun parseCompetitionSection(bodyHtml: String, preferences: Preferences?) {
        for (match in competitionPattern.findAll(bodyHtml)) {
            val contest = match.groupValues[1]
            val left = if (match.groupValues[2] == "Won!") 0
            else match.groupValues[3].toIntOrNull() ?: -1
            preferences?.setInt("nsContestants$contest", left)
        }
    }

    internal fun parseWarSection(bodyHtml: String): String? = when {
        bodyHtml.contains("You led the filthy hippies to victory", ignoreCase = true) ||
            bodyHtml.contains("You led the Orcish frat boys to victory", ignoreCase = true) ||
            bodyHtml.contains("You started a chain of events", ignoreCase = true) ->
            QuestDatabase.FINISHED
        bodyHtml.contains(
            "You've managed to get the war between the hippies and frat boys started",
            ignoreCase = true,
        ) -> "step1"
        bodyHtml.contains(
            "The Council has gotten word of tensions building between the hippies and the frat boys",
            ignoreCase = true,
        ) -> QuestDatabase.STARTED
        else -> null
    }

    internal fun parseOracleSection(bodyHtml: String, preferences: Preferences?) {
        val match = oracleTargetPattern.find(bodyHtml) ?: return
        val target = match.groupValues[1].trim()
        if (target.isNotBlank()) preferences?.setString("sourceOracleTarget", target)
    }

    internal fun parseGhostSection(bodyHtml: String, preferences: Preferences?) {
        val match = ghostTargetPattern.find(bodyHtml) ?: return
        val location = match.groupValues[1].trim()
        if (location.isNotBlank()) preferences?.setString("ghostLocation", location)
    }

    internal fun parseNewYouSection(bodyHtml: String, preferences: Preferences?) {
        val match = newYouPattern.find(bodyHtml) ?: return
        preferences?.setString("_newYouQuestSkill", match.groupValues[1].trim())
        preferences?.setString("_newYouQuestSharpensDone", match.groupValues[2].trim())
        preferences?.setString("_newYouQuestSharpensToDo", match.groupValues[3].trim())
        preferences?.setString("_newYouQuestMonster", match.groupValues[4].trim())
    }

    internal fun parseShenSection(bodyHtml: String, preferences: Preferences?) {
        val item = shenPattern.find(bodyHtml)?.groupValues?.get(1)?.trim()
            ?: shen2Pattern.find(bodyHtml)?.groupValues?.get(1)?.trim()
            ?: return
        preferences?.setString("shenQuestItem", item)
    }

    internal fun parseHippyFratSection(bodyHtml: String, preferences: Preferences?) {
        val match = hippyFratPattern.find(bodyHtml) ?: return
        val hippiesLeft = match.groupValues[1].toIntOrNull() ?: return
        val fratsLeft = match.groupValues[2].toIntOrNull() ?: return
        preferences?.setInt("hippiesDefeated", 333 - hippiesLeft)
        preferences?.setInt("fratboysDefeated", 333 - fratsLeft)
    }

    private fun applyTelegram(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.TELEGRAM) == QuestDatabase.UNSTARTED) return false
        val step = parseTelegramSection(text, preferences) ?: return false
        return advanceIfBetter(questDatabase, Quest.TELEGRAM, step)
    }

    private fun applyPartyFair(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.PARTY_FAIR) == QuestDatabase.UNSTARTED) return false
        val step = parsePartyFairSection(text, preferences, gameDatabase) ?: return false
        advanceIfBetter(questDatabase, Quest.PARTY_FAIR, step)
        return true
    }

    private fun resolvePartyFairProgress(
        text: String,
        step: PartyFairStep,
        gameDatabase: GameDatabase?,
    ): String? = when (step.subQuest) {
        "trash" -> partyFairTrashPattern.find(text)?.groupValues?.get(1)?.replace(",", "")
        "partiers" -> partyFairPartiersPattern.find(text)?.groupValues?.get(1)
        "meat", "dj" -> partyFairMeatPattern.find(text)?.groupValues?.get(1)?.replace(",", "")
        "booze" -> partyFairItemProgress(text, step, partyFairBoozePattern1, partyFairBoozePattern2, gameDatabase)
        "food" -> partyFairItemProgress(text, step, partyFairFoodPattern1, partyFairFoodPattern2, gameDatabase)
        "woots" -> when (step.step) {
            "step2" -> "100"
            else -> partyFairWootsPattern.find(text)?.groupValues?.get(1)
        }
        else -> step.progress
    }

    private fun partyFairItemProgress(
        text: String,
        step: PartyFairStep,
        pattern1: Regex,
        pattern2: Regex,
        gameDatabase: GameDatabase?,
    ): String? {
        if (step.progress == "") return ""
        val match = when {
            step.step == "step2" -> pattern2.find(text)
            else -> pattern1.find(text)
        } ?: return null
        val qty = match.groupValues[1]
        val itemId = gameDatabase?.item(match.groupValues[2].trim())?.id ?: 0
        return if (itemId > 0) "$qty $itemId" else null
    }

    private fun applyDoctorBag(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (completeDoctorBagDelivery(text, questDatabase, preferences)) return true
        val step = parseDoctorBagSection(text, preferences) ?: return false
        if (step == QuestDatabase.STARTED &&
            questDatabase.getProgress(Quest.DOCTOR_BAG) == QuestDatabase.UNSTARTED
        ) {
            questDatabase.setProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED)
            return true
        }
        return advanceIfBetter(questDatabase, Quest.DOCTOR_BAG, step)
    }

    private fun applyGuzzlr(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (completeGuzzlrDelivery(text, questDatabase, preferences)) return true
        val step = parseGuzzlrSection(text, preferences, gameDatabase) ?: return false
        if (step == QuestDatabase.STARTED &&
            questDatabase.getProgress(Quest.GUZZLR) == QuestDatabase.UNSTARTED
        ) {
            questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.STARTED)
            return true
        }
        return advanceIfBetter(questDatabase, Quest.GUZZLR, step)
    }

    private fun applyRufus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        val step = parseRufusSection(text, preferences, gameDatabase) ?: return false
        return advanceIfBetter(questDatabase, Quest.RUFUS, step)
    }

    private fun applyPeakStatus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (!text.contains("Peak", ignoreCase = true) &&
            !text.contains("Highland Lord", ignoreCase = true)
        ) {
            return false
        }
        if (!questDatabase.isAtLeast(Quest.TOPPING, "step2")) return false
        val step = parsePeakSection(text, preferences) ?: return false
        advanceIfBetter(questDatabase, Quest.TOPPING, step)
        return true
    }

    private fun applyPrimordial(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.PRIMORDIAL) == QuestDatabase.UNSTARTED &&
            !text.contains("Primordial Soup", ignoreCase = true)
        ) return false
        val step = parsePrimordialSection(text, preferences) ?: return false
        if (questDatabase.getProgress(Quest.PRIMORDIAL) == QuestDatabase.UNSTARTED &&
            step != QuestDatabase.STARTED
        ) {
            questDatabase.setProgress(Quest.PRIMORDIAL, QuestDatabase.STARTED)
        }
        return advanceIfBetter(questDatabase, Quest.PRIMORDIAL, step)
    }

    private fun applyFinalQuestLog(text: String, questDatabase: QuestDatabase): Boolean {
        if (questDatabase.getProgress(Quest.FINAL) == QuestDatabase.UNSTARTED) return false
        val step = parseFinalSection(text) ?: return false
        return advanceIfBetter(questDatabase, Quest.FINAL, step)
    }

    private fun applyCompetitionStatus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        parseCompetitionSection(text, preferences)
        var changed = competitionPattern.containsMatchIn(text)
        if (text.contains("Naughty Sorceress' Tower", ignoreCase = true) &&
            text.contains("Ascend the", ignoreCase = true)
        ) {
            if (advanceIfBetter(questDatabase, Quest.FINAL, "step6")) changed = true
        }
        return changed
    }

    private fun applyOracle(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.ORACLE) == QuestDatabase.UNSTARTED &&
            !text.contains("Oracle", ignoreCase = true)
        ) return false
        parseOracleSection(text, preferences)
        if (questDatabase.getProgress(Quest.ORACLE) == QuestDatabase.UNSTARTED) {
            questDatabase.setProgress(Quest.ORACLE, QuestDatabase.STARTED)
            return true
        }
        return oracleTargetPattern.containsMatchIn(text)
    }

    private fun applyGhost(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.GHOST) == QuestDatabase.UNSTARTED &&
            !text.contains("ghost", ignoreCase = true)
        ) return false
        parseGhostSection(text, preferences)
        if (questDatabase.getProgress(Quest.GHOST) == QuestDatabase.UNSTARTED) {
            questDatabase.setProgress(Quest.GHOST, QuestDatabase.STARTED)
            return true
        }
        return ghostTargetPattern.containsMatchIn(text)
    }

    private fun applyNewYou(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.NEW_YOU) == QuestDatabase.UNSTARTED) return false
        parseNewYouSection(text, preferences)
        return newYouPattern.containsMatchIn(text)
    }

    private fun applyShen(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.SHEN) == QuestDatabase.UNSTARTED) return false
        parseShenSection(text, preferences)
        return shenPattern.containsMatchIn(text) || shen2Pattern.containsMatchIn(text)
    }

    private fun applyHippyFratStatus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.HIPPY_FRAT) == QuestDatabase.UNSTARTED) return false
        parseHippyFratSection(text, preferences)
        return hippyFratPattern.containsMatchIn(text)
    }

    private val finalQuestLogSteps = listOf(
        "Go investigate the weird contest" to "step1",
        "Defeat the other entrants in the Naughty Sorceress" to "step2",
        "Go talk to the contest official" to "step3",
        "You're the big winner" to "step4",
        "Attend your coronation" to "step4",
        "treacherous hedge maze" to "step5",
        "Get through the door at the base" to "step6",
        "Continue climbing the Naughty Sorceress" to "step10",
        "Continue your ascent of the Naughty Sorceress" to "step11",
        "Confront the Naughty Sorceress" to "step12",
        "wand of Nagamar" to "step13",
        "Free King Ralph from his prism prison" to "step14",
        "You freed the Kingdom of the tyranny of the Naughty Sorceress" to QuestDatabase.FINISHED,
    )

    internal fun appendCyrusAdjective(preferences: Preferences?, adjective: String) {
        if (adjective.isBlank() || preferences == null) return
        val current = preferences.getString("cyrusAdjectives", "")
        if (current.contains(adjective, ignoreCase = true)) return
        preferences.setString(
            "cyrusAdjectives",
            if (current.isBlank()) adjective else "$current,$adjective",
        )
    }

    private fun advanceIfBetter(questDatabase: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = questDatabase.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) <= QuestDatabase.stepOrdinal(current)) return false
        questDatabase.setProgress(quest, step)
        return true
    }

    internal fun abandonDoctorBag(questDatabase: QuestDatabase, preferences: Preferences?): Boolean {
        clearDoctorBagQuest(preferences)
        if (questDatabase.getProgress(Quest.DOCTOR_BAG) == QuestDatabase.UNSTARTED) return false
        questDatabase.setProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED)
        return true
    }

    internal fun completeDoctorBagDelivery(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        val deliverySignal = text.contains("green lights", ignoreCase = true) ||
            text.contains("bag has been permanently upgraded", ignoreCase = true) ||
            text.contains("lights go dark again", ignoreCase = true)
        if (!deliverySignal) return false
        when {
            text.contains("One of the five green lights", ignoreCase = true) ->
                preferences?.setInt("doctorBagQuestLights", 1)
            text.contains("second of the five green lights", ignoreCase = true) ->
                preferences?.setInt("doctorBagQuestLights", 2)
            text.contains("third of the five green lights", ignoreCase = true) ->
                preferences?.setInt("doctorBagQuestLights", 3)
            text.contains("fourth of the five green lights", ignoreCase = true) ->
                preferences?.setInt("doctorBagQuestLights", 4)
            text.contains("lights go dark again", ignoreCase = true) ->
                preferences?.setInt("doctorBagQuestLights", 0)
        }
        if (text.contains("bag has been permanently upgraded", ignoreCase = true)) {
            preferences?.let { prefs ->
                prefs.setInt("doctorBagUpgrades", prefs.getInt("doctorBagUpgrades") + 1)
            }
        }
        clearDoctorBagQuest(preferences)
        questDatabase.setProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED)
        return true
    }

    internal fun abandonGuzzlr(questDatabase: QuestDatabase, preferences: Preferences?): Boolean {
        preferences?.setBoolean("_guzzlrQuestAbandoned", true)
        GuzzlrCombatSync.clearQuestPrefs(preferences)
        if (questDatabase.getProgress(Quest.GUZZLR) == QuestDatabase.UNSTARTED) return false
        questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED)
        return true
    }

    internal fun completeGuzzlrDelivery(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase? = null,
        hasItemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean = GuzzlrCombatSync.completeDelivery(
        text = text,
        questDatabase = questDatabase,
        preferences = preferences,
        gameDatabase = gameDatabase,
        hasItemCount = hasItemCount,
        consumeItem = consumeItem,
    )

    private fun clearDoctorBagQuest(preferences: Preferences?) {
        preferences?.setString("doctorBagQuestItem", "")
        preferences?.setString("doctorBagQuestLocation", "")
    }

    private val GUZZLR_PLATINUM_ITEM_IDS = GuzzlrCombatSync.GUZZLR_PLATINUM_ITEM_IDS
}
