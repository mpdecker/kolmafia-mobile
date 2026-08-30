package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.HaciendaManager

/** Desktop ChoiceControl hacienda choices 410–418 + 440 (Phases 3351–3365). */
object HaciendaChoiceSync {

    val CHOICE_IDS = setOf(
        HaciendaManager.CHOICE_HALLWAY,
        HaciendaManager.CHOICE_HALLWAY_LEFT,
        HaciendaManager.CHOICE_HALLWAY_RIGHT,
    ) + HaciendaManager.ROOM_CHOICE_IDS + HaciendaManager.RECORDING_CHOICE

    fun applyPostChoice(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        choiceUrl: String = "",
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (preferences == null) return false
        when (choiceId) {
            in HaciendaManager.ROOM_CHOICE_IDS ->
                HaciendaManager.parseRoom(choiceId, decision, html, preferences, questDatabase, sessionLog)
            HaciendaManager.RECORDING_CHOICE -> {
                if (decision == 1) {
                    HaciendaManager.parseRecording(choiceUrl, html, preferences)
                }
            }
        }
        return choiceId in CHOICE_IDS
    }

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || choiceId != HaciendaManager.RECORDING_CHOICE) return false
        HaciendaManager.preRecording(html, preferences)
        return true
    }
}
