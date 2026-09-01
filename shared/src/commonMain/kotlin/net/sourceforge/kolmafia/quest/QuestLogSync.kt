package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.QuestCouncilDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.shop.DesertBeachUnlockSync

/** Shared quest-log sync triggers from KoL page/adventure response text. */
object QuestLogSync {

    data class QuestSyncContext(
        val hasItemId: (Int) -> Boolean = { false },
        val place: String? = null,
        val url: String? = null,
        val preferences: Preferences? = null,
        val currentRun: Int = 0,
        val gameDatabase: GameDatabase? = null,
        val consumeItem: (itemId: Int, quantity: Int) -> Unit = { _, _ -> },
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
        "Manual of Labor",
        "Manual of Transmission",
        "Manual of Dexterity",
        "The Tomb is within the Misspelled",
        "Clownlord Beelzebozo",
        "Eleven inches",
        "captured poltersandwich",
        "stole my own pants",
        "war between the hippies",
        "led the filthy hippies to victory",
        "led the Orcish frat boys to victory",
        "Remaining soldiers:",
        "Ron Copperhead",
        "Holy MacGuffin",
        "Nemesis",
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
        QuestSpecialSync.apply(responseText, questDatabase, context.preferences, context.gameDatabase)
        TowerSync.parseTower(responseText, questDatabase, context.preferences)
        if (context.url?.contains("council.php", ignoreCase = true) == true) {
            QuestCouncilDatabase.handleCouncilText(responseText, questDatabase)
        }
        applyFantasyRealmHooks(responseText, context)
        applyPlaceHooks(context.place, questDatabase, context, responseText)
        if (shouldSync(responseText)) {
            questLogRequest?.syncAll()
        }
        applyDerivedQuestStatus(questDatabase, context.preferences, context.currentRun)
    }

    /** Cross-quest status links mirrored from desktop QuestLogRequest. */
    fun applyDerivedQuestStatus(
        questDatabase: QuestDatabase,
        preferences: Preferences? = null,
        ascensionNumber: Int = 0,
    ) {
        if (questDatabase.isAtLeast(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED) &&
            !questDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)
        ) {
            questDatabase.setProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED)
        }
        if (questDatabase.isAtLeast(Quest.SPOOKYRAVEN_BABIES, QuestDatabase.STARTED) &&
            !questDatabase.isQuestFinished(Quest.SPOOKYRAVEN_DANCE)
        ) {
            questDatabase.setProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED)
        }
        if (questDatabase.isQuestLaterThan(Quest.MANOR, QuestDatabase.STARTED) &&
            !questDatabase.isQuestFinished(Quest.SPOOKYRAVEN_DANCE)
        ) {
            questDatabase.setProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED)
        }
        if (questDatabase.isAtLeast(Quest.PYRAMID, QuestDatabase.STARTED) &&
            !questDatabase.isQuestFinished(Quest.DESERT)
        ) {
            questDatabase.setProgress(Quest.DESERT, QuestDatabase.FINISHED)
        }
        if (questDatabase.isAtLeast(Quest.MACGUFFIN, "step1") &&
            !questDatabase.isQuestFinished(Quest.BLACK)
        ) {
            questDatabase.setProgress(Quest.BLACK, QuestDatabase.FINISHED)
        }
        if (questDatabase.isQuestLaterThan(Quest.WORSHIP, "step3")) {
            for (quest in listOf(Quest.CURSES, Quest.DOCTOR, Quest.BUSINESS, Quest.SPARE)) {
                if (!questDatabase.isQuestFinished(quest)) {
                    questDatabase.setProgress(quest, QuestDatabase.FINISHED)
                }
            }
        }
        preferences?.let { prefs ->
            applyDerivedQuestPrefs(questDatabase, prefs, ascensionNumber)
        }
    }

    private fun applyDerivedQuestPrefs(
        questDatabase: QuestDatabase,
        preferences: Preferences,
        ascensionNumber: Int,
    ) {
        preferences.setBoolean(
            "middleChamberUnlock",
            questDatabase.isQuestLaterThan(Quest.PYRAMID, QuestDatabase.STARTED),
        )
        preferences.setBoolean(
            "lowerChamberUnlock",
            questDatabase.isQuestLaterThan(Quest.PYRAMID, "step1"),
        )
        preferences.setBoolean(
            "controlRoomUnlock",
            questDatabase.isQuestLaterThan(Quest.PYRAMID, "step2"),
        )
        if (!preferences.getBoolean("pyramidBombUsed", false)) {
            preferences.setBoolean("pyramidBombUsed", questDatabase.isQuestFinished(Quest.PYRAMID))
        }
        preferences.setBoolean(
            "bigBrotherRescued",
            questDatabase.isQuestLaterThan(Quest.SEA_MONKEES, "step1"),
        )
        when {
            questDatabase.isQuestFinished(Quest.ISLAND_WAR) ->
                preferences.setString("warProgress", "finished")
            questDatabase.isQuestLaterThan(Quest.ISLAND_WAR, QuestDatabase.STARTED) ->
                preferences.setString("warProgress", "started")
            else -> preferences.setString("warProgress", "unstarted")
        }
        if (questDatabase.isAtLeast(Quest.PIRATE, QuestDatabase.STARTED) ||
            questDatabase.isQuestFinished(Quest.HIPPY)
        ) {
            preferences.setInt("lastIslandUnlock", ascensionNumber)
        }
        if (questDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)) {
            preferences.setInt("lastSecondFloorUnlock", ascensionNumber)
        }
        if (questDatabase.isQuestLaterThan(Quest.GARBAGE, "step7")) {
            preferences.setInt("lastCastleGroundUnlock", ascensionNumber)
        }
        if (questDatabase.isQuestLaterThan(Quest.GARBAGE, "step8")) {
            preferences.setInt("lastCastleTopUnlock", ascensionNumber)
        }
        if (questDatabase.isQuestLaterThan(Quest.WORSHIP, "step1")) {
            preferences.setInt("lastTempleButtonsUnlock", ascensionNumber)
            preferences.setInt("lastTempleUnlock", ascensionNumber)
        }
        if (questDatabase.isQuestLaterThan(Quest.MACGUFFIN, "step1") ||
            questDatabase.isQuestFinished(Quest.MEATCAR)
        ) {
            DesertBeachUnlockSync.setAvailable(ascensionNumber, preferences)
        }
    }

    internal fun applyPlaceHooks(
        place: String?,
        questDatabase: QuestDatabase,
        context: QuestSyncContext,
        html: String = "",
    ) {
        when (place?.lowercase()) {
            "fern", "fernruin" -> applyFernTowerUnlock(questDatabase, context)
            "paco" -> applyFactoryTurnIn(questDatabase, context)
            "ocg" -> {
                applyEgoKeyTurnIn(questDatabase, context, html)
                applyEgoBookTurnIn(questDatabase, context, html)
            }
            "challenge" -> applyGuildChallengeTurnIn(questDatabase, context, html)
            "scg" -> applyNemesisVisitSteps(questDatabase, context)
            "bigisland" -> {
                if (questDatabase.getProgress(Quest.ISLAND_WAR) == QuestDatabase.UNSTARTED) {
                    questDatabase.setProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED)
                }
            }
            "warehouse" -> {
                val current = questDatabase.getProgress(Quest.WAREHOUSE)
                if (current == QuestDatabase.UNSTARTED) {
                    questDatabase.setProgress(Quest.WAREHOUSE, QuestDatabase.STARTED)
                }
            }
            "partyfair" -> {
                if (questDatabase.getProgress(Quest.PARTY_FAIR) == QuestDatabase.UNSTARTED) {
                    questDatabase.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
                }
            }
        }
    }

    private fun applyFantasyRealmHooks(
        responseText: String,
        context: QuestSyncContext,
    ) {
        if (responseText.contains("snarfblat=507")) {
            context.preferences?.setBoolean(FR_CEMETERY_UNLOCKED_PREF, true)
        }
    }

    private fun applyFernTowerUnlock(questDatabase: QuestDatabase, context: QuestSyncContext) {
        if (!context.hasItemId(FERNSWARTHY_KEY_ID)) return
        if (questDatabase.getProgress(Quest.EGO) == "step2") {
            questDatabase.setProgress(Quest.EGO, "step3")
        }
    }

    internal fun applyEgoBookTurnIn(
        questDatabase: QuestDatabase,
        context: QuestSyncContext,
        html: String = "",
    ) {
        val place = context.place?.lowercase() ?: return
        if (place != "ocg") return
        if (!context.hasItemId(DUSTY_BOOK_ID)) return
        val current = questDatabase.getProgress(Quest.EGO)
        if (current != "step6" && current != QuestDatabase.FINISHED) return
        val hasTurnInHtml = html.contains("Manual of Labor", ignoreCase = true) ||
            html.contains("Manual of Transmission", ignoreCase = true) ||
            html.contains("Manual of Dexterity", ignoreCase = true)
        if (!hasTurnInHtml) return
        if (QuestDatabase.stepOrdinal(QuestDatabase.FINISHED) > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(Quest.EGO, QuestDatabase.FINISHED)
        }
        consumeEgoBookTurnInItems(context)
    }

    internal fun consumeEgoBookTurnInItems(context: QuestSyncContext) {
        if (context.hasItemId(DUSTY_BOOK_ID)) {
            context.consumeItem(DUSTY_BOOK_ID, 1)
        }
        if (context.hasItemId(FERNSWARTHY_KEY_ID)) {
            context.consumeItem(FERNSWARTHY_KEY_ID, 1)
        }
    }

    internal fun applyEgoKeyTurnIn(
        questDatabase: QuestDatabase,
        context: QuestSyncContext,
        html: String = "",
    ) {
        val place = context.place?.lowercase() ?: return
        if (place != "ocg") return
        if (!context.hasItemId(FERNSWARTHY_KEY_ID)) return
        val current = questDatabase.getProgress(Quest.EGO)
        if (current != QuestDatabase.STARTED) return
        val hasTurnInHtml = html.contains("hand over Fernswarthy's key", ignoreCase = true) ||
            html.contains("returned with Fernswarthy's key", ignoreCase = true) ||
            html.contains("takes Fernswarthy's key", ignoreCase = true)
        if (!hasTurnInHtml) return
        if (QuestDatabase.stepOrdinal("step1") > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(Quest.EGO, "step1")
            context.consumeItem(FERNSWARTHY_KEY_ID, 1)
        }
    }

    internal fun applyGuildChallengeTurnIn(
        questDatabase: QuestDatabase,
        context: QuestSyncContext,
        html: String = "",
    ) {
        val place = context.place?.lowercase() ?: return
        if (place != "challenge") return

        if (html.contains("Eleven inches", ignoreCase = true) &&
            context.hasItemId(QuestItemRules.BIG_KNOB_SAUSAGE_ID)
        ) {
            val current = questDatabase.getProgress(Quest.MUSCLE)
            if (QuestDatabase.stepOrdinal(QuestDatabase.FINISHED) > QuestDatabase.stepOrdinal(current)) {
                questDatabase.setProgress(Quest.MUSCLE, QuestDatabase.FINISHED)
                context.consumeItem(QuestItemRules.BIG_KNOB_SAUSAGE_ID, 1)
            }
        }

        if (html.contains("captured poltersandwich", ignoreCase = true) &&
            context.hasItemId(QuestItemRules.EXORCISED_SANDWICH_ID)
        ) {
            val current = questDatabase.getProgress(Quest.MYST)
            if (QuestDatabase.stepOrdinal(QuestDatabase.FINISHED) > QuestDatabase.stepOrdinal(current)) {
                questDatabase.setProgress(Quest.MYST, QuestDatabase.FINISHED)
                context.consumeItem(QuestItemRules.EXORCISED_SANDWICH_ID, 1)
            }
        }

        if (html.contains("stole my own pants", ignoreCase = true)) {
            val current = questDatabase.getProgress(Quest.MOXIE)
            if (QuestDatabase.stepOrdinal(QuestDatabase.FINISHED) > QuestDatabase.stepOrdinal(current)) {
                questDatabase.setProgress(Quest.MOXIE, QuestDatabase.FINISHED)
            }
        }
    }

    internal fun applyFactoryTurnIn(questDatabase: QuestDatabase, context: QuestSyncContext) {
        if (!context.hasItemId(FACTORY_ENVELOPE_ID)) return
        val current = questDatabase.getProgress(Quest.FACTORY)
        if (QuestDatabase.stepOrdinal(QuestDatabase.FINISHED) > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(Quest.FACTORY, QuestDatabase.FINISHED)
            context.consumeItem(FACTORY_ENVELOPE_ID, 1)
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

    /** dusty old book — guild EGO finish turn-in item */
    const val DUSTY_BOOK_ID = 2279

    /** Guild class manuals — desktop ItemPool MUS/MYS/MOX_MANUAL */
    const val MUS_MANUAL_ID = 2280
    const val MYS_MANUAL_ID = 2281
    const val MOX_MANUAL_ID = 2282

    val GUILD_PLACES = setOf("paco", "ocg", "scg", "challenge")

    const val FR_CEMETERY_UNLOCKED_PREF = "frCemetaryUnlocked"
}
