package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.TurnCounter

/** Shared quest-log sync triggers from KoL page/adventure response text. */
object QuestLogSync {

    data class QuestSyncContext(
        val hasItemId: (Int) -> Boolean = { false },
        val place: String? = null,
        val preferences: Preferences? = null,
        val currentRun: Int = 0,
    )

    val SYNC_SIGNALS = listOf(
        "Quest Completed",
        "Quest Updated",
        "added to your Quest Log",
        "Your quest log has been updated",
        "You've solved the rat problem",
        "Well done!  You have slain the Boss Bat",
        "Thank you for slaying the Goblin King",
        "Thanks for the larva",
        "old guy by the sea",
        "I lost my favorite boot",
        "The old man snores fitfully",
        "The Sea Monkees",
        "wish my big brother was here",
        "Party Fair",
        "telegram for you",
        "doctor bag",
        "Pirate Realm",
        "You grab an eyepatch",
        "an envelope with your name on it",
        "Degrassi Knoll",
        "South of the Border",
        "Whitey's Grove",
        "7-foot Dwarves",
        "the location of the Cemetary",
        "Fernswarthy's key",
        "unlocked Fernswarthy's tower",
        "dusty old book",
        "Manual of Dexterity",
        "The Tomb is within the Misspelled",
        "Clownlord Beelzebozo",
        "Eleven inches",
        "captured poltersandwich",
        "stole my own pants",
    )

    fun shouldSync(responseText: String): Boolean =
        SYNC_SIGNALS.any { responseText.contains(it, ignoreCase = true) }

    suspend fun processResponse(
        responseText: String,
        questDatabase: QuestDatabase,
        questLogRequest: QuestLogRequest?,
        context: QuestSyncContext = QuestSyncContext(),
    ) {
        QuestAdvanceRules.apply(responseText, questDatabase)
        applyPlaceHooks(context.place, questDatabase, context)
        if (shouldSync(responseText)) {
            questLogRequest?.syncAll()
        }
    }

    internal fun applyPlaceHooks(
        place: String?,
        questDatabase: QuestDatabase,
        context: QuestSyncContext,
    ) {
        applyEgoKeyTurnIn(questDatabase, context)
        when (place?.lowercase()) {
            "fern" -> applyFernTowerUnlock(questDatabase, context)
            "paco" -> applyFactoryFinish(questDatabase, context)
            "scg" -> applyNemesisVisitSteps(questDatabase, context)
        }
    }

    private fun applyFernTowerUnlock(questDatabase: QuestDatabase, context: QuestSyncContext) {
        if (!context.hasItemId(FERNSWARTHY_KEY_ID)) return
        if (questDatabase.getProgress(Quest.EGO) == "step2") {
            questDatabase.setProgress(Quest.EGO, "step3")
        }
    }

    private fun applyEgoKeyTurnIn(questDatabase: QuestDatabase, context: QuestSyncContext) {
        val place = context.place?.lowercase() ?: return
        if (place !in GUILD_PLACES) return
        if (!context.hasItemId(FERNSWARTHY_KEY_ID)) return
        val current = questDatabase.getProgress(Quest.EGO)
        if (current != QuestDatabase.STARTED) return
        if (QuestDatabase.stepOrdinal("step1") > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(Quest.EGO, "step1")
        }
    }

    private fun applyFactoryFinish(questDatabase: QuestDatabase, context: QuestSyncContext) {
        if (!context.hasItemId(FACTORY_ENVELOPE_ID)) return
        val current = questDatabase.getProgress(Quest.FACTORY)
        if (QuestDatabase.stepOrdinal(QuestDatabase.FINISHED) > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(Quest.FACTORY, QuestDatabase.FINISHED)
        }
    }

    private fun applyNemesisVisitSteps(questDatabase: QuestDatabase, context: QuestSyncContext) {
        when (questDatabase.getProgress(Quest.NEMESIS)) {
            "step8" -> questDatabase.setProgress(Quest.NEMESIS, "step9")
            "step16" -> questDatabase.setProgress(Quest.NEMESIS, "step16.5")
            "step16.5" -> {
                questDatabase.setProgress(Quest.NEMESIS, "step17")
                context.preferences?.let {
                    TurnCounter.startNemesisAssassinUnlock(it, context.currentRun)
                }
            }
        }
    }

    /** thick padded envelope — guild FACTORY turn-in item */
    const val FACTORY_ENVELOPE_ID = 3201

    /** Fernswarthy's key — guild EGO turn-in item */
    const val FERNSWARTHY_KEY_ID = 2277

    val GUILD_PLACES = setOf("paco", "ocg", "scg", "challenge")
}
