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
    ) {
        QuestAdvanceRules.apply(responseText, questDatabase)
        if (shouldSync(responseText)) {
            questLogRequest?.syncAll()
        }
    }
}
