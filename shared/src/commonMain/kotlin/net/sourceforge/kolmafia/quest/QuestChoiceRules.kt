package net.sourceforge.kolmafia.quest

/** Quest step bumps from choice adventure response text. */
object QuestChoiceRules {

    fun apply(choiceId: Int, responseText: String, questDatabase: QuestDatabase): Boolean {
        var advanced = false
        when (choiceId) {
            930 -> {
                if (responseText.contains("lucky rabbit's foot", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, QuestDatabase.FINISHED) || advanced
                } else if (responseText.contains("White Citadel", ignoreCase = true) ||
                    responseText.contains("satchel", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, QuestDatabase.STARTED) || advanced
                }
            }
            932 -> {
                if (responseText.contains("White Citadel", ignoreCase = true)) {
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
