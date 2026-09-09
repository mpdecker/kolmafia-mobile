package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] LT&T Office choices 1171–1175.
 */
object TelegramChoiceSync {

    const val OFFICE = 1171
    const val BEGINS = 1172
    const val CONTINUES = 1173
    const val CONTINUES_AGAIN = 1174
    const val CONCLUDES = 1175

    private val TELEGRAM_PATTERN = Regex("""value="RE: (.*?)"""")

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        visitHtml: String?,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase == null) return false
        return when (choiceId) {
            OFFICE -> applyOffice(decision, visitHtml ?: html, questDatabase, preferences)
            BEGINS -> {
                questDatabase.setProgress(Quest.TELEGRAM, "step1")
                preferences?.setInt("lttQuestStageCount", 0)
                true
            }
            CONTINUES -> {
                questDatabase.setProgress(Quest.TELEGRAM, "step2")
                preferences?.setInt("lttQuestStageCount", 0)
                true
            }
            CONTINUES_AGAIN -> {
                questDatabase.setProgress(Quest.TELEGRAM, "step3")
                preferences?.setInt("lttQuestStageCount", 0)
                true
            }
            CONCLUDES -> {
                questDatabase.setProgress(Quest.TELEGRAM, "step4")
                preferences?.setInt("lttQuestStageCount", 0)
                true
            }
            else -> false
        }
    }

    /** Office visit: capture RE: quest option names before accepting. */
    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (choiceId != OFFICE || preferences == null) return false
        val matches = TELEGRAM_PATTERN.findAll(html).map { it.groupValues[1] }.toList()
        if (matches.isEmpty()) return false
        // Preserve current name if already set; otherwise seed first option for scripts.
        if (preferences.getString("lttQuestName", "").isBlank()) {
            preferences.setString("lttQuestName", matches.first())
        }
        preferences.setString("_lttQuestOptions", matches.joinToString("|"))
        return true
    }

    private fun applyOffice(
        decision: Int,
        visitHtml: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
    ): Boolean {
        if (decision < 4) {
            questDatabase.setProgress(Quest.TELEGRAM, QuestDatabase.STARTED)
            preferences?.setInt("lttQuestDifficulty", decision)
            preferences?.setInt("lttQuestStageCount", 0)
            bindQuestNameOnAccept(decision, visitHtml, preferences)
            return true
        }
        if (decision == 5) {
            questDatabase.setProgress(Quest.TELEGRAM, QuestDatabase.UNSTARTED)
            preferences?.setInt("lttQuestDifficulty", 0)
            preferences?.setInt("lttQuestStageCount", 0)
            preferences?.setString("lttQuestName", "")
            return true
        }
        return false
    }

    /**
     * Bind [lttQuestName] from visit HTML RE: options, falling back to the
     * `_lttQuestOptions` list captured on [applyVisit].
     */
    private fun bindQuestNameOnAccept(
        decision: Int,
        visitHtml: String,
        preferences: Preferences?,
    ) {
        if (preferences == null || decision <= 0) return
        val fromHtml = TELEGRAM_PATTERN.findAll(visitHtml).map { it.groupValues[1] }.toList()
        if (decision <= fromHtml.size) {
            preferences.setString("lttQuestName", fromHtml[decision - 1])
            return
        }
        val fromPref = preferences.getString("_lttQuestOptions", "")
            .split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (decision <= fromPref.size) {
            preferences.setString("lttQuestName", fromPref[decision - 1])
        }
    }
}
