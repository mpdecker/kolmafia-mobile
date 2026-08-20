package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BugbearManager
import net.sourceforge.kolmafia.session.CryptManager

/** Quest step bumps from combat win/loss and item drops. */
object QuestFightRules {

    data class QuestCombatResult(
        val advanced: Boolean,
        val resyncQuestLogPage1: Boolean = false,
    )

    const val VOLCANO_MAP_ID = 3291
    const val BURNOUTS_DEFEATED_PREF = "burnoutsDefeated"
    const val INTIMIDATING_CHAINSAW_ID = 9972
    private const val BURNOUTS_GOAL = 30

    private val VOLCANIC_CAVE_MARKER = "(volcanic cave)"

    private val PARTY_FAIR_MONSTERS = setOf(
        "biker", "\"plain\" girl", "jock", "party girl", "burnout",
    )

    private val TELEGRAM_STAGE_MONSTERS = setOf(
        "drunk cowpoke", "surly gambler", "wannabe gunslinger", "cow cultist",
        "hired gun", "camp cook", "skeletal gunslinger", "restless ghost",
        "buzzard", "mountain lion", "grizzled bear", "diamondback rattler",
        "coal snake", "frontwinder", "caugr", "pyrobove", "spidercow", "moomy",
    )

    private val TELEGRAM_BOSS_MONSTERS = setOf(
        "jeff the fancy skeleton", "daisy the unclean", "pecos dave",
        "pharaoh amoon-ra cowtep", "snake-eyes glenn", "former sheriff dan driscoll",
        "unusual construct", "clara", "granny hackleton",
    )

    private val GHOST_BOSS_MONSTERS = setOf(
        "the ghost of oily mcbindle",
        "boneless blobghost",
        "the ghost of monsieur baguelle",
        "the headless horseman",
        "the icewoman",
        "the ghost of ebenoozer screege",
        "the ghost of lord montague spookyraven",
        "the ghost of vanillica \"trashblossom\" gorton",
        "the ghost of sam mcgee",
        "the ghost of richard cockingham",
        "the ghost of waldo the carpathian",
        "emily koops, a spooky lime",
        "the ghost of jim unfortunato",
    )

    fun applyFightStarted(questDatabase: QuestDatabase, monster: String): Boolean {
        if (monster.isBlank()) return false
        var advanced = false
        if (monster.contains(VOLCANIC_CAVE_MARKER, ignoreCase = true)) {
            advanced = advance(questDatabase, Quest.NEMESIS, "step28") || advanced
        }
        if (monster.equals("cake lord", ignoreCase = true)) {
            advanced = advance(questDatabase, Quest.ARMORER, "step2") || advanced
        }
        return advanced
    }

    fun applyCombat(
        questDatabase: QuestDatabase,
        monster: String,
        won: Boolean,
        itemsGained: List<String> = emptyList(),
        itemIdsGained: List<Int> = emptyList(),
        preferences: Preferences? = null,
        adventureId: String = "",
        responseText: String = "",
        hasItemEquipped: (Int) -> Boolean = { false },
        hasItemId: (Int) -> Boolean = { false },
        ascensionNumber: Int = 0,
        combatItemId: Int? = null,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        currentRun: Int = 0,
    ): QuestCombatResult {
        var advanced = false
        var resyncQuestLogPage1 = false
        if (FightItemPrefSync.apply(
                html = responseText,
                monster = monster,
                preferences = preferences,
                combatItemId = combatItemId,
                consumeItem = consumeItem,
                currentRun = currentRun,
            )
        ) {
            advanced = true
        }
        if (CryptManager.handleFightEvilness(responseText, adventureId, preferences)) {
            advanced = true
        }
        if (FireExtinguisherCombatSync.apply(responseText, adventureId, preferences, questDatabase)) {
            advanced = true
        }
        if (BugbearManager.handleKeyotron(responseText, monster, preferences)) {
            advanced = true
        }
        if (NewYouCombatSync.apply(responseText, questDatabase, preferences)) {
            advanced = true
        }
        if (won && adventureId == PirateRealmSync.PIRATEREALM_ISLAND_ADVENTURE.toString()) {
            advanced = PirateRealmSync.applyIslandCombatWin(questDatabase, preferences) || advanced
        }
        if (monster.isNotBlank()) {
            nemesisStep(monster, won)?.let {
                if (advance(questDatabase, Quest.NEMESIS, it)) advanced = true
            }
            citadelStep(monster, won, preferences, responseText)?.let {
                if (advance(questDatabase, Quest.CITADEL, it)) advanced = true
            }
            armorerStep(monster, won)?.let {
                if (advance(questDatabase, Quest.ARMORER, it)) advanced = true
            }
            if (won) {
                if (applyTelegramCombat(questDatabase, monster, preferences)) advanced = true
                if (applyGhostBossReset(questDatabase, monster, preferences)) advanced = true
                applyPartyFairCombat(
                    questDatabase, monster, preferences, responseText, hasItemEquipped,
                )?.let { partyFair ->
                    if (partyFair.advanced) advanced = true
                    if (partyFair.resyncQuestLogPage1) resyncQuestLogPage1 = true
                }
                if (QuestCombatWinExtrasSync.apply(
                        monster, questDatabase, preferences, ascensionNumber,
                    )
                ) {
                    advanced = true
                }
                if (ZeppelinRonSync.applyCabinProgress(
                        monster, responseText, questDatabase, preferences,
                    )
                ) {
                    advanced = true
                }
                if (PirateRealmSync.applyNamedBossWin(monster, questDatabase, preferences)) {
                    advanced = true
                }
                if (SeaCombatSync.apply(monster, questDatabase, preferences, hasItemEquipped)) {
                    advanced = true
                }
                if (AirportCombatSync.apply(monster, responseText, questDatabase, preferences)) {
                    advanced = true
                }
                if (SmutOrcCombatSync.apply(monster, responseText, preferences)) {
                    advanced = true
                }
                if (CryptManager.defeatBoss(monster, questDatabase, preferences)) {
                    advanced = true
                }
            } else {
                QuestFightLostSync.apply(monster, responseText, questDatabase, preferences)
            }
        }
        if (won) {
            if (DailyDungeonCombatSync.apply(adventureId, preferences)) advanced = true
            if (WalfordBucketCombatSync.apply(adventureId, responseText, questDatabase, preferences)) {
                advanced = true
            }
            if (QuestLocationCombatSync.apply(adventureId, monster, preferences)) {
                advanced = true
            }
            if (DinseyCombatSync.apply(adventureId, responseText, questDatabase, preferences)) {
                advanced = true
            }
            if (ManorDrawerCombatSync.apply(adventureId, responseText, preferences, hasItemId)) {
                advanced = true
            }
            if (MerkinColosseumCombatSync.apply(adventureId, monster, preferences)) {
                advanced = true
            }
        }
        if (itemsGained.any { it.contains("volcano map", ignoreCase = true) } ||
            VOLCANO_MAP_ID in itemIdsGained
        ) {
            if (advance(questDatabase, Quest.NEMESIS, "step25")) advanced = true
        }
        return QuestCombatResult(advanced, resyncQuestLogPage1)
    }

    private data class PartyFairCombatResult(
        val advanced: Boolean = false,
        val resyncQuestLogPage1: Boolean = false,
    )

    private fun applyTelegramCombat(
        questDatabase: QuestDatabase,
        monster: String,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase.getProgress(Quest.TELEGRAM) == QuestDatabase.UNSTARTED) return false
        val lower = monster.lowercase()
        if (lower in TELEGRAM_BOSS_MONSTERS) {
            questDatabase.setProgress(Quest.TELEGRAM, QuestDatabase.UNSTARTED)
            preferences?.setString("lttQuestName", "")
            preferences?.setInt("lttQuestDifficulty", 0)
            preferences?.setInt("lttQuestStageCount", 0)
            return true
        }
        if (lower in TELEGRAM_STAGE_MONSTERS) {
            preferences?.setInt(
                "lttQuestStageCount",
                (preferences?.getInt("lttQuestStageCount", 0) ?: 0) + 1,
            )
            return true
        }
        return false
    }

    internal fun applyGhostBossReset(
        questDatabase: QuestDatabase,
        monster: String,
        preferences: Preferences?,
    ): Boolean {
        if (monster.trim().lowercase() !in GHOST_BOSS_MONSTERS) return false
        questDatabase.setProgress(Quest.GHOST, QuestDatabase.UNSTARTED)
        preferences?.setString("ghostLocation", "")
        return true
    }

    private fun applyPartyFairCombat(
        questDatabase: QuestDatabase,
        monster: String,
        preferences: Preferences?,
        responseText: String,
        hasItemEquipped: (Int) -> Boolean,
    ): PartyFairCombatResult? {
        val lower = monster.lowercase()
        if (lower !in PARTY_FAIR_MONSTERS) return null
        val prefs = preferences ?: return null
        val freeTurns = (prefs.getInt("_neverendingPartyFreeTurns", 0) + 1).coerceAtMost(10)
        prefs.setInt("_neverendingPartyFreeTurns", freeTurns)
        val progress = questDatabase.getProgress(Quest.PARTY_FAIR)
        if (progress != "step1" && progress != "step2") return PartyFairCombatResult()
        when (prefs.getString("_questPartyFairQuest", "")) {
            "partiers" -> {
                val kills = if (hasItemEquipped(INTIMIDATING_CHAINSAW_ID)) 2 else 1
                val remaining = (prefs.getString("_questPartyFairProgress", "0").toIntOrNull() ?: 0) - kills
                prefs.setString("_questPartyFairProgress", remaining.coerceAtLeast(0).toString())
                if (remaining < 1) {
                    return PartyFairCombatResult(advanced = advance(questDatabase, Quest.PARTY_FAIR, "step2"))
                }
            }
            "dj" -> {
                val match = QuestSpecialSync.partyFairDjMeatPattern.find(responseText) ?: return null
                val meat = match.groupValues[1].replace(",", "").toIntOrNull() ?: return null
                val remaining = (prefs.getString("_questPartyFairProgress", "0").toIntOrNull() ?: 0) - meat
                prefs.setString("_questPartyFairProgress", remaining.coerceAtLeast(0).toString())
                if (remaining < 1) {
                    return PartyFairCombatResult(advanced = advance(questDatabase, Quest.PARTY_FAIR, "step2"))
                }
            }
            "trash" -> {
                val match = QuestSpecialSync.partyFairCombatTrashPattern.find(responseText) ?: return null
                val trash = match.groupValues[1].toIntOrNull() ?: return null
                val remaining = (prefs.getString("_questPartyFairProgress", "0").toIntOrNull() ?: 0) - trash
                prefs.setString("_questPartyFairProgress", remaining.coerceAtLeast(0).toString())
                if (remaining < 1) {
                    return PartyFairCombatResult(advanced = advance(questDatabase, Quest.PARTY_FAIR, "step2"))
                }
            }
            "woots" -> return PartyFairCombatResult(resyncQuestLogPage1 = true)
            else -> return null
        }
        return PartyFairCombatResult(advanced = true)
    }

    private fun nemesisStep(monster: String, won: Boolean): String? {
        val lower = monster.lowercase()
        if (won) {
            if (lower.contains(VOLCANIC_CAVE_MARKER)) return "step29"
            if (lower.startsWith("the unknown ")) return "step2"
            return when (lower) {
                "clownlord beelzebozo" -> "step6"
                "menacing thug" -> "step19"
                "mob penguin hitman" -> "step21"
                "hunting seal", "turtle trapper", "evil spaghetti cult assassin",
                "béarnaise zombie", "flock of seagulls", "mariachi bandolero" -> "step23"
                else -> null
            }
        }
        return when (lower) {
            "menacing thug" -> "step18"
            "mob penguin hitman" -> "step20"
            "hunting seal", "turtle trapper", "evil spaghetti cult assassin",
            "béarnaise zombie", "flock of seagulls", "mariachi bandolero" -> "step22"
            "argarggagarg the dire hellseal", "safari jack, small-game hunter",
            "yakisoba the executioner", "heimandatz, nacho golem", "jocko homo",
            "the mariachi with no name" -> "step24"
            else -> null
        }
    }

    private fun citadelStep(
        monster: String,
        won: Boolean,
        preferences: Preferences?,
        responseText: String = "",
    ): String? {
        if (!won) return null
        val lower = monster.lowercase()
        if (lower == "pair of burnouts") {
            val prefs = preferences ?: return "step4"
            val increment = if (responseText.contains("throw the opium grenade")) 3 else 1
            val next = (prefs.getInt(BURNOUTS_DEFEATED_PREF, 0) + increment).coerceAtMost(BURNOUTS_GOAL)
            prefs.setInt(BURNOUTS_DEFEATED_PREF, next)
            return if (next >= BURNOUTS_GOAL) "step4" else null
        }
        return when (lower) {
            "biclops" -> "step5"
            "surprised and annoyed witch", "extremely annoyed witch" -> "step7"
            "elpízo & crosybdis", "elpizo & crosybdis" -> "step10"
            else -> null
        }
    }

    private fun armorerStep(monster: String, won: Boolean): String? {
        if (!won) return null
        return if (monster.equals("cake lord", ignoreCase = true)) "step3" else null
    }

    private fun advance(questDatabase: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = questDatabase.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(quest, step)
            return true
        }
        return false
    }
}
