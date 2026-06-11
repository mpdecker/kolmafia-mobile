package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.request.QuestLogRequest

/** Shared quest-log sync triggers from KoL page/adventure response text. */
object QuestLogSync {

    val SYNC_SIGNALS = listOf(
        "Quest Completed",
        "Quest Updated",
        "added to your Quest Log",
        "Your quest log has been updated",
        "You've solved the rat problem",
        "Well done!  You have slain the Boss Bat",
        "Thank you for slaying the Goblin King",
        "Thanks for the larva",
    )

    fun shouldSync(responseText: String): Boolean =
        SYNC_SIGNALS.any { responseText.contains(it, ignoreCase = true) }

    suspend fun processResponse(
        responseText: String,
        questDatabase: QuestDatabase,
        questLogRequest: QuestLogRequest?,
    ) {
        QuestAdvanceRules.apply(responseText, questDatabase)
        if (shouldSync(responseText)) {
            questLogRequest?.syncAll()
        }
    }
}
