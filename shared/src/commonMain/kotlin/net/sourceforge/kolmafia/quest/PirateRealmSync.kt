package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Pirate Realm place-response sync. Mirrors desktop [QuestManager.handlePirateRealmChange].
 */
object PirateRealmSync {

    const val PIRATEREALM_ISLAND_ADVENTURE = 531

    fun getPirateRealmIslandNumber(questDatabase: QuestDatabase): Int {
        if (!questDatabase.isAtLeast(Quest.PIRATEREALM, "step7")) return 0
        if (!questDatabase.isAtLeast(Quest.PIRATEREALM, "step12")) return 1
        return 2
    }

    fun stepForIslandProgress(island: Int, progress: Int): String =
        "step${island * 5 + 2 + progress}"

    fun setPirateRealmIslandQuestProgress(
        questDatabase: QuestDatabase,
        progress: Int,
    ): Boolean = setPirateRealmIslandQuestProgress(
        questDatabase,
        getPirateRealmIslandNumber(questDatabase),
        progress,
    )

    fun setPirateRealmIslandQuestProgress(
        questDatabase: QuestDatabase,
        island: Int,
        progress: Int,
    ): Boolean = advanceIfBetter(questDatabase, Quest.PIRATEREALM, stepForIslandProgress(island, progress))

    fun applyChoice(
        choiceId: Int,
        responseText: String,
        decision: Int,
        optionLabel: String?,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        var advanced = false
        when (choiceId) {
            1347 -> {
                val crewmate = preferences.getString("_pirateRealmCrewmate$decision", "")
                if (crewmate.isNotBlank()) {
                    preferences.setString("_pirateRealmCrewmate", crewmate)
                }
            }
            1348 -> {
                if (decision in 1..6) {
                    preferences.setString("_pirateRealmCurio", (10189 + decision).toString())
                }
            }
            1349 -> {
                val (ship, speed) = pirateRealmShipForDecision(decision)
                if (ship.isNotBlank()) preferences.setString("_pirateRealmShip", ship)
                if (speed > 0) preferences.setInt("_pirateRealmShipSpeed", speed)
            }
            1352, 1353, 1354 -> {
                val island = choiceId - 1352
                advanced = setPirateRealmIslandQuestProgress(questDatabase, island, 0) || advanced
                preferences.setInt("_pirateRealmIslandMonstersDefeated", 0)
                preferences.setInt("_pirateRealmSailingTurns", 0)
                preferences.setBoolean("_pirateRealmWindicleUsed", false)
                val label = optionLabel?.trim().orEmpty()
                if (label.isNotBlank() && !label.equals("Decide Later", ignoreCase = true)) {
                    preferences.setString("_lastPirateRealmIsland", label)
                }
            }
            1355 -> {
                advanced = setPirateRealmIslandQuestProgress(questDatabase, 2) || advanced
            }
            1360 -> {
                if (decision == 6) {
                    advanced = incrementSailingTurns(preferences, questDatabase) || advanced
                }
                if (decision == 5 && responseText.contains("You gain 500 gold", ignoreCase = true)) {
                    preferences.setBoolean("_pirateRealmSoldCompass", true)
                }
            }
            1362 -> {
                if (decision != 0) {
                    advanced = incrementSailingTurns(preferences, questDatabase) || advanced
                }
                if (decision == 2 && responseText.contains("you manage to outsail the storm", ignoreCase = true)) {
                    advanced = incrementSailingTurns(preferences, questDatabase) || advanced
                    incrementCappedPref(preferences, "pirateRealmStormsEscaped", max = 10)
                }
            }
            1364 -> {
                if (decision != 0) {
                    advanced = incrementSailingTurns(preferences, questDatabase) || advanced
                }
                if (decision == 1 && responseText.contains("blast them to bits", ignoreCase = true)) {
                    incrementCappedPref(preferences, "pirateRealmShipsDestroyed", max = 10)
                }
            }
            1365 -> {
                if (decision != 0) {
                    advanced = incrementSailingTurns(preferences, questDatabase) || advanced
                }
                if (decision == 1 && responseText.contains("plush sea serpent", ignoreCase = true)) {
                    preferences.setBoolean("pirateRealmUnlockedPlushie", true)
                }
            }
            1356, 1357, 1358, 1359, 1361, 1363 -> {
                if (decision != 0) {
                    advanced = incrementSailingTurns(preferences, questDatabase) || advanced
                }
            }
            1369, 1370, 1371, 1385 -> {
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step6") || advanced
            }
            1372 -> {
                preferences.setBoolean("pirateRealmUnlockedRhum", true)
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step6") || advanced
            }
            1375 -> {
                preferences.setBoolean("pirateRealmUnlockedShavingCream", true)
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step11") || advanced
            }
            1376, 1377 -> {
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step11") || advanced
            }
            1379 -> {
                if (responseText.contains("Island Drinkin' skillbook", ignoreCase = true)) {
                    preferences.setBoolean("pirateRealmUnlockedTikiSkillbook", true)
                    advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step16") || advanced
                }
            }
            1380 -> {
                preferences.setBoolean("pirateRealmUnlockedTattoo", true)
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step16") || advanced
            }
            1383 -> {
                preferences.setBoolean("pirateRealmUnlockedThirdCrewmate", true)
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step11") || advanced
            }
            1384 -> {
                preferences.setBoolean("pirateRealmUnlockedAnemometer", true)
                advanced = advanceIfBetter(questDatabase, Quest.PIRATEREALM, "step16") || advanced
            }
        }
        return advanced
    }

    fun applyIslandCombatWin(
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        val prefs = preferences ?: return false
        val defeated = prefs.getInt("_pirateRealmIslandMonstersDefeated", 0) + 1
        prefs.setInt("_pirateRealmIslandMonstersDefeated", defeated)
        val threshold = if (getPirateRealmIslandNumber(questDatabase) < 2) 4 else 9
        return if (defeated == threshold) {
            setPirateRealmIslandQuestProgress(questDatabase, 3)
        } else {
            false
        }
    }

    fun applyWindicleUse(
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        responseText: String,
        runawaySuccess: Boolean = false,
    ): Boolean {
        val prefs = preferences ?: return false
        if (responseText.contains("This item can only be used in PirateRealm", ignoreCase = true)) {
            return false
        }
        prefs.setBoolean("_pirateRealmWindicleUsed", true)
        if (!responseText.contains("Your foe is blown clear of the island", ignoreCase = true) &&
            !runawaySuccess
        ) {
            return false
        }
        val defeated = prefs.getInt("_pirateRealmIslandMonstersDefeated", 0) + 3
        prefs.setInt("_pirateRealmIslandMonstersDefeated", defeated)
        val threshold = if (getPirateRealmIslandNumber(questDatabase) < 2) 4 else 9
        return if (defeated >= threshold) {
            setPirateRealmIslandQuestProgress(questDatabase, 3)
        } else {
            false
        }
    }

    fun applyWindicleFromFightHtml(
        fightHtml: String,
        adventureId: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (adventureId != PIRATEREALM_ISLAND_ADVENTURE.toString()) return false
        if (!fightHtml.contains("windicle", ignoreCase = true) &&
            !fightHtml.contains("blown clear of the island", ignoreCase = true)
        ) {
            return false
        }
        val runawaySuccess = fightHtml.contains("You run away", ignoreCase = true)
        return applyWindicleUse(questDatabase, preferences, fightHtml, runawaySuccess)
    }

    private fun incrementSailingTurns(
        preferences: Preferences,
        questDatabase: QuestDatabase,
    ): Boolean {
        val turns = preferences.getInt("_pirateRealmSailingTurns", 0) + 1
        preferences.setInt("_pirateRealmSailingTurns", turns)
        val speed = preferences.getInt("_pirateRealmShipSpeed", 0)
        return if (speed > 0 && turns >= speed) {
            setPirateRealmIslandQuestProgress(questDatabase, 1)
        } else {
            false
        }
    }

    private fun pirateRealmShipForDecision(decision: Int): Pair<String, Int> = when (decision) {
        1 -> "Rigged Frigate" to 7
        2 -> "Intimidating Galleon" to 7
        3 -> "Speedy Caravel" to 6
        4 -> "Swift Clipper" to 4
        5 -> "Menacing Man o' War" to 9
        else -> "" to 0
    }

    private fun incrementCappedPref(preferences: Preferences, key: String, max: Int) {
        val next = (preferences.getInt(key, 0) + 1).coerceAtMost(max)
        preferences.setInt(key, next)
    }

    fun parseResponse(
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (preferences != null && !preferences.getBoolean("prAlways", false)) {
            preferences.setBoolean("_prToday", true)
        }

        val step = when {
            html.contains("You grab an eyepatch") -> QuestDatabase.STARTED
            html.contains("sail1.gif") -> "step1"
            html.contains("sail2.gif") -> "step6"
            html.contains("sail3.gif") -> "step11"
            html.contains("an envelope with your name on it") -> QuestDatabase.FINISHED
            else -> return false
        }

        if (step == QuestDatabase.FINISHED) {
            questDatabase.setProgress(Quest.PIRATEREALM, QuestDatabase.FINISHED)
            applyFinishUnlocks(html, preferences)
            return true
        }

        return advanceIfBetter(questDatabase, Quest.PIRATEREALM, step)
    }

    private fun applyFinishUnlocks(html: String, preferences: Preferences?) {
        if (preferences == null) return
        if (html.contains("piratical blunderbuss")) {
            preferences.setBoolean("pirateRealmUnlockedBlunderbuss", true)
        }
        if (html.contains("pirate fork")) {
            preferences.setBoolean("pirateRealmUnlockedFork", true)
        }
        if (html.contains("Scurvy and Sobriety Prevention")) {
            preferences.setBoolean("pirateRealmUnlockedScurvySkillbook", true)
        }
        if (html.contains("lucky gold ring")) {
            preferences.setBoolean("pirateRealmUnlockedGoldRing", true)
        }
        if (html.contains("Menacing Man o' War")) {
            preferences.setBoolean("pirateRealmUnlockedManOWar", true)
        }
        if (html.contains("Swift Clipper")) {
            preferences.setBoolean("pirateRealmUnlockedClipper", true)
        }
    }

    private fun advanceIfBetter(questDatabase: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = questDatabase.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) <= QuestDatabase.stepOrdinal(current)) return false
        questDatabase.setProgress(quest, step)
        return true
    }
}
