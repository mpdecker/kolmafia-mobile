package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogEntry
import net.sourceforge.kolmafia.preferences.Preferences

/** Quest-log section progress routing mirroring desktop QuestDatabase.findQuestProgress. */
object QuestLogProgress {

    fun findQuestProgress(
        prefKey: String,
        bodyHtml: String,
        entry: QuestLogEntry,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): String? {
        when (prefKey) {
            Quest.PIRATEREALM.prefKey -> return null
            Quest.ISLAND_WAR.prefKey -> return QuestSpecialSync.parseWarSection(bodyHtml)
            Quest.PIRATE.prefKey -> {
                if (bodyHtml.contains(
                        "Oh, and also you've managed to scam your way belowdecks, which is cool",
                        ignoreCase = true,
                    )
                ) {
                    return QuestDatabase.FINISHED
                }
            }
            Quest.TOPPING.prefKey -> {
                if (bodyHtml.contains("The Highland Lord wants you to light", ignoreCase = true)) {
                    return QuestSpecialSync.parsePeakSection(bodyHtml, preferences)
                }
            }
            Quest.FINAL.prefKey -> {
                QuestSpecialSync.parseCompetitionSection(bodyHtml, preferences)
                QuestSpecialSync.parseFinalSection(bodyHtml)?.let { return it }
            }
            Quest.TELEGRAM.prefKey ->
                return QuestSpecialSync.parseTelegramSection(bodyHtml, preferences)
            Quest.PARTY_FAIR.prefKey ->
                return QuestSpecialSync.parsePartyFairSection(bodyHtml, preferences, gameDatabase)
            Quest.DOCTOR_BAG.prefKey ->
                return QuestSpecialSync.parseDoctorBagSection(bodyHtml, preferences)
            Quest.GUZZLR.prefKey ->
                return QuestSpecialSync.parseGuzzlrSection(bodyHtml, preferences, gameDatabase)
            Quest.RUFUS.prefKey ->
                return QuestSpecialSync.parseRufusSection(bodyHtml, preferences, gameDatabase)
            Quest.PRIMORDIAL.prefKey ->
                return QuestSpecialSync.parsePrimordialSection(bodyHtml, preferences)
            Quest.ORACLE.prefKey -> QuestSpecialSync.parseOracleSection(bodyHtml, preferences)
            Quest.GHOST.prefKey -> QuestSpecialSync.parseGhostSection(bodyHtml, preferences)
            Quest.NEW_YOU.prefKey -> QuestSpecialSync.parseNewYouSection(bodyHtml, preferences)
            Quest.SHEN.prefKey -> QuestSpecialSync.parseShenSection(bodyHtml, preferences)
            Quest.HIPPY_FRAT.prefKey -> QuestSpecialSync.parseHippyFratSection(bodyHtml, preferences)
        }
        return QuestLogDatabase.detectStep(entry, bodyHtml)
    }
}
