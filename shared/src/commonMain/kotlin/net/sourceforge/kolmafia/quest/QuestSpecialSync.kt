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
    private val partyFairTrashPattern = Regex("""Trash left: ~(.*?) pieces""")
    private val partyFairWootsPattern = Regex("""Hype level: (\d+) / 100 megawoots""")
    private val partyFairPartiersPattern = Regex("""Partiers remaining: (\d+)""")
    private val partyFairMeatPattern = Regex("""Remaining bill: (.*?) Meat""")
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

    private fun applyTelegram(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.TELEGRAM) == QuestDatabase.UNSTARTED) return false
        for (step in telegramSteps) {
            if (!text.contains(step.signal, ignoreCase = true)) continue
            preferences?.setString("lttQuestName", step.questName)
            preferences?.setInt("lttQuestDifficulty", step.difficulty)
            return advanceIfBetter(questDatabase, Quest.TELEGRAM, step.step)
        }
        return false
    }

    private fun applyPartyFair(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.PARTY_FAIR) == QuestDatabase.UNSTARTED) return false
        for (step in partyFairSteps) {
            if (!text.contains(step.signal, ignoreCase = true)) continue
            preferences?.setString("_questPartyFairQuest", step.subQuest)
            val progress = resolvePartyFairProgress(text, step, gameDatabase)
            if (progress != null) {
                preferences?.setString("_questPartyFairProgress", progress)
            }
            advanceIfBetter(questDatabase, Quest.PARTY_FAIR, step.step)
            return true
        }
        return false
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

    private fun applyOracle(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.ORACLE) == QuestDatabase.UNSTARTED &&
            !text.contains("Oracle", ignoreCase = true)
        ) return false
        val match = oracleTargetPattern.find(text) ?: return false
        val target = match.groupValues[1].trim()
        if (target.isBlank()) return false
        preferences?.setString("sourceOracleTarget", target)
        if (questDatabase.getProgress(Quest.ORACLE) == QuestDatabase.UNSTARTED) {
            questDatabase.setProgress(Quest.ORACLE, QuestDatabase.STARTED)
            return true
        }
        return false
    }

    private fun applyGhost(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.GHOST) == QuestDatabase.UNSTARTED &&
            !text.contains("ghost", ignoreCase = true)
        ) return false
        val match = ghostTargetPattern.find(text) ?: return false
        val location = match.groupValues[1].trim()
        if (location.isBlank()) return false
        preferences?.setString("ghostLocation", location)
        if (questDatabase.getProgress(Quest.GHOST) == QuestDatabase.UNSTARTED) {
            questDatabase.setProgress(Quest.GHOST, QuestDatabase.STARTED)
            return true
        }
        return true
    }

    private fun applyNewYou(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.NEW_YOU) == QuestDatabase.UNSTARTED) return false
        val match = newYouPattern.find(text) ?: return false
        preferences?.setString("_newYouQuestSkill", match.groupValues[1].trim())
        preferences?.setString("_newYouQuestSharpensDone", match.groupValues[2].trim())
        preferences?.setString("_newYouQuestSharpensToDo", match.groupValues[3].trim())
        preferences?.setString("_newYouQuestMonster", match.groupValues[4].trim())
        return true
    }

    private fun applyShen(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.SHEN) == QuestDatabase.UNSTARTED) return false
        val item = shenPattern.find(text)?.groupValues?.get(1)?.trim()
            ?: shen2Pattern.find(text)?.groupValues?.get(1)?.trim()
            ?: return false
        preferences?.setString("shenQuestItem", item)
        return true
    }

    private fun applyDoctorBag(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (completeDoctorBagDelivery(text, questDatabase, preferences)) return true
        if (text.contains("Acquire ", ignoreCase = true)) {
            val match = doctorBagItemPattern.find(text) ?: return false
            preferences?.setString("doctorBagQuestItem", match.groupValues[1].trim())
            if (questDatabase.getProgress(Quest.DOCTOR_BAG) == QuestDatabase.UNSTARTED) {
                questDatabase.setProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED)
            }
            return true
        }
        if (questDatabase.getProgress(Quest.DOCTOR_BAG) == QuestDatabase.UNSTARTED &&
            !text.contains("doctor", ignoreCase = true)
        ) return false
        if (text.contains("to the patient", ignoreCase = true)) {
            val match = doctorBagLocationPattern.find(text) ?: return false
            preferences?.setString("doctorBagQuestItem", match.groupValues[1].trim())
            preferences?.setString("doctorBagQuestLocation", match.groupValues[2].trim())
            return advanceIfBetter(questDatabase, Quest.DOCTOR_BAG, "step1")
        }
        return false
    }

    private fun applyGuzzlr(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (completeGuzzlrDelivery(text, questDatabase, preferences)) return true
        if (text.contains("Craft a personalized Guzzlr cocktail.", ignoreCase = true)) {
            preferences?.setString("guzzlrQuestTier", "platinum")
            if (questDatabase.getProgress(Quest.GUZZLR) == QuestDatabase.UNSTARTED) {
                questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.STARTED)
            }
            return true
        }
        if (text.contains("to your Guzzlr client", ignoreCase = true)) {
            val match = guzzlrLocationPattern.find(text) ?: return false
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
            return advanceIfBetter(questDatabase, Quest.GUZZLR, "step1")
        }
        if (text.contains("Acquire ", ignoreCase = true) &&
            text.contains("Guzzlr client", ignoreCase = true)
        ) {
            val match = guzzlrBoozePattern.find(text) ?: return false
            preferences?.setString("guzzlrQuestBooze", match.groupValues[1].trim())
            if (questDatabase.getProgress(Quest.GUZZLR) == QuestDatabase.UNSTARTED) {
                questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.STARTED)
            }
            return true
        }
        return false
    }

    private fun applyRufus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (!text.contains("Rufus wants you", ignoreCase = true) &&
            !text.contains("Call Rufus", ignoreCase = true)
        ) {
            return false
        }
        val prefs = preferences ?: return false
        val step = RufusManager(prefs).handleQuestLog(text, gameDatabase) ?: return false
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
        if (questDatabase.getProgress(Quest.MACGUFFIN) == QuestDatabase.UNSTARTED) return false

        var updated = false
        when {
            text.contains("lit the fire on A-Boo Peak", ignoreCase = true) -> {
                preferences?.setInt("booPeakProgress", 0)
                preferences?.setBoolean("booPeakLit", true)
                updated = true
            }
            text.contains("check out A-Boo Peak", ignoreCase = true) -> {
                preferences?.setInt("booPeakProgress", 100)
                updated = true
            }
            else -> booPeakPattern.find(text)?.let { match ->
                preferences?.setInt("booPeakProgress", match.groupValues[1].toIntOrNull() ?: 0)
                updated = true
            }
        }

        if (text.contains("lit the fire on Twin Peak", ignoreCase = true)) {
            preferences?.setInt("twinPeakProgress", 15)
            updated = true
        }

        when {
            text.contains("lit the fire on Oil Peak", ignoreCase = true) -> {
                preferences?.setBoolean("oilPeakLit", true)
                preferences?.setString("oilPeakProgress", "0")
                updated = true
            }
            text.contains("go to Oil Peak and investigate", ignoreCase = true) -> {
                preferences?.setString("oilPeakProgress", "310.66")
                updated = true
            }
            else -> oilPeakPattern.find(text)?.let { match ->
                preferences?.setString("oilPeakProgress", match.groupValues[1])
                updated = true
            }
        }

        if (!updated) return false

        val peaksComplete = preferences?.getBoolean("booPeakLit") == true &&
            preferences.getInt("twinPeakProgress") == 15 &&
            preferences.getBoolean("oilPeakLit") == true
        return if (peaksComplete) {
            advanceIfBetter(questDatabase, Quest.MACGUFFIN, "step3")
        } else {
            advanceIfBetter(questDatabase, Quest.MACGUFFIN, "step2")
        }
    }

    private fun applyPrimordial(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.PRIMORDIAL) == QuestDatabase.UNSTARTED &&
            !text.contains("Primordial Soup", ignoreCase = true)
        ) return false
        var step: String? = null
        when {
            text.contains("creating an unstoppable supervirus", ignoreCase = true) ->
                step = QuestDatabase.FINISHED
            text.contains(
                "finding your way to a higher, warmer, oranger part of the Primordial Soup",
                ignoreCase = true,
            ) -> step = "step1"
            text.contains("ran into a virus named Cyrus", ignoreCase = true) ->
                step = "step2"
            text.contains("floating aimlessly in the Primordial Soup", ignoreCase = true) ->
                step = QuestDatabase.STARTED
        }
        cyrusAdjectivePattern.find(text)?.let { match ->
            val adjectives = match.groupValues[1].replace(" and", ",").split(", ")
            for (adjective in adjectives) {
                appendCyrusAdjective(preferences, adjective.trim())
            }
        }
        if (step == null) return cyrusAdjectivePattern.containsMatchIn(text)
        if (questDatabase.getProgress(Quest.PRIMORDIAL) == QuestDatabase.UNSTARTED &&
            step != QuestDatabase.STARTED
        ) {
            questDatabase.setProgress(Quest.PRIMORDIAL, QuestDatabase.STARTED)
        }
        return advanceIfBetter(questDatabase, Quest.PRIMORDIAL, step)
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

    private fun applyFinalQuestLog(text: String, questDatabase: QuestDatabase): Boolean {
        if (questDatabase.getProgress(Quest.FINAL) == QuestDatabase.UNSTARTED) return false
        for ((signal, step) in finalQuestLogSteps) {
            if (!text.contains(signal, ignoreCase = true)) continue
            return advanceIfBetter(questDatabase, Quest.FINAL, step)
        }
        return false
    }

    private fun applyCompetitionStatus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        var changed = false
        for (match in competitionPattern.findAll(text)) {
            val contest = match.groupValues[1]
            val left = if (match.groupValues[2] == "Won!") 0
            else match.groupValues[3].toIntOrNull() ?: -1
            preferences?.setInt("nsContestants$contest", left)
            changed = true
        }
        if (text.contains("Naughty Sorceress' Tower", ignoreCase = true) &&
            text.contains("Ascend the", ignoreCase = true)
        ) {
            if (advanceIfBetter(questDatabase, Quest.FINAL, "step6")) changed = true
        }
        return changed
    }

    private fun appendCyrusAdjective(preferences: Preferences?, adjective: String) {
        if (adjective.isBlank() || preferences == null) return
        val current = preferences.getString("cyrusAdjectives", "")
        if (current.contains(adjective, ignoreCase = true)) return
        preferences.setString(
            "cyrusAdjectives",
            if (current.isBlank()) adjective else "$current,$adjective",
        )
    }

    private fun applyHippyFratStatus(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.HIPPY_FRAT) == QuestDatabase.UNSTARTED) return false
        val match = hippyFratPattern.find(text) ?: return false
        val hippiesLeft = match.groupValues[1].toIntOrNull() ?: return false
        val fratsLeft = match.groupValues[2].toIntOrNull() ?: return false
        preferences?.setInt("hippiesDefeated", 333 - hippiesLeft)
        preferences?.setInt("fratboysDefeated", 333 - fratsLeft)
        return true
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
        clearGuzzlrQuest(preferences)
        if (questDatabase.getProgress(Quest.GUZZLR) == QuestDatabase.UNSTARTED) return false
        questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED)
        return true
    }

    internal fun completeGuzzlrDelivery(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (!text.contains("You finally manage to track down", ignoreCase = true)) return false
        val tier = preferences?.getString("guzzlrQuestTier", "")?.lowercase().orEmpty()
        if (tier.isNotBlank()) {
            val key = when (tier) {
                "bronze" -> "guzzlrBronzeDeliveries"
                "gold" -> "guzzlrGoldDeliveries"
                "platinum" -> "guzzlrPlatinumDeliveries"
                else -> null
            }
            key?.let { deliveryKey ->
                preferences?.let { prefs ->
                    prefs.setInt(deliveryKey, prefs.getInt(deliveryKey) + 1)
                }
            }
        }
        preferences?.setInt("guzzlrDeliveryProgress", 0)
        clearGuzzlrQuest(preferences)
        questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED)
        return true
    }

    private fun clearDoctorBagQuest(preferences: Preferences?) {
        preferences?.setString("doctorBagQuestItem", "")
        preferences?.setString("doctorBagQuestLocation", "")
    }

    private fun clearGuzzlrQuest(preferences: Preferences?) {
        preferences?.setString("guzzlrQuestBooze", "")
        preferences?.setString("guzzlrQuestClient", "")
        preferences?.setString("guzzlrQuestLocation", "")
        preferences?.setString("guzzlrQuestTier", "")
    }

    private val GUZZLR_PLATINUM_ITEM_IDS = 10541..10545
}
