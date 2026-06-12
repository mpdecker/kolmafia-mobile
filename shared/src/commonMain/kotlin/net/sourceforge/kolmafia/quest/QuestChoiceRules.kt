package net.sourceforge.kolmafia.quest

/** Quest step bumps from choice adventure response text. */
object QuestChoiceRules {

    fun apply(
        choiceId: Int,
        responseText: String,
        questDatabase: QuestDatabase,
        decision: Int = 0,
    ): Boolean {
        var advanced = false
        when (choiceId) {
            189 -> {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step26") || advanced
            }
            542 -> {
                if (responseText.contains("oddly chilly", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.MOXIE, "step1") || advanced
                }
            }
            930 -> {
                if (responseText.contains("lucky rabbit's foot", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, QuestDatabase.FINISHED) || advanced
                } else if (responseText.contains("White Citadel", ignoreCase = true) ||
                    responseText.contains("satchel", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, QuestDatabase.STARTED) || advanced
                }
            }
            931 -> {
                if (responseText.contains("Life Ain't Nothin But Witches and Mummies", ignoreCase = true) ||
                    responseText.contains("Witches and Mummies", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, "step6") || advanced
                }
            }
            932 -> {
                if (responseText.contains("No Whammies", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, "step8") || advanced
                } else if (responseText.contains("steel your nerves", ignoreCase = true) ||
                    responseText.contains("White Citadel", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, "step9") || advanced
                }
            }
            1049 -> {
                if (responseText.contains("Epic Weapon's yours", ignoreCase = true) ||
                    responseText.contains("Epic Weapon is yours", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step3") || advanced
                } else if (responseText.contains("Epic Weapon", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step4") || advanced
                } else if (responseText.contains("ghost", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step1") || advanced
                }
            }
            1061 -> {
                if (responseText.contains("Heart of Madness", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.ARMORER, "step1") || advanced
                }
            }
            1065 -> {
                if (decision == 1 || decision == 3) {
                    advanced = setIfBetter(questDatabase, Quest.ARMORER, QuestDatabase.STARTED) || advanced
                }
            }
            1064 -> {
                when (decision) {
                    1 -> advanced = setIfBetter(questDatabase, Quest.DOC, QuestDatabase.STARTED) || advanced
                    2 -> advanced = setIfBetter(questDatabase, Quest.DOC, QuestDatabase.FINISHED) || advanced
                }
            }
            1087 -> {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step11") || advanced
                if (responseText.contains("passed", ignoreCase = true) ||
                    responseText.contains("continue", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step12") || advanced
                }
            }
            1088 -> {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step13") || advanced
                if (responseText.contains("BOOOOOOM", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step15") || advanced
                }
            }
        }
        return advanced
    }

    private fun setIfBetter(db: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = db.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            db.setProgress(quest, step)
            return true
        }
        return false
    }
}
