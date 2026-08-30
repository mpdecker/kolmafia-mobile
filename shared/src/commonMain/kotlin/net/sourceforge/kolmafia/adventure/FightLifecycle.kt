package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Headless state machine for the parts of desktop FightRequest that surround
 * HTML/result synchronization. It deliberately owns no UI or network code.
 */
class FightLifecycle {
    data class Context(
        val round: Int,
        val action: FightAction?,
        val inMultiFight: Boolean,
        val choiceFollowsFight: Boolean,
        val fightEnded: Boolean,
        val lastResponse: String,
    )

    private var round = 0
    private var action: FightAction? = null
    private var inMultiFight = false
    private var choiceFollowsFight = false
    private var lastResponse = ""

    val context: Context
        get() = Context(
            round = round,
            action = action,
            inMultiFight = inMultiFight,
            choiceFollowsFight = choiceFollowsFight,
            fightEnded = round > 0 && !inMultiFight,
            lastResponse = lastResponse,
        )

    fun beginFight(followsChoice: Boolean = false) {
        round = 0
        action = null
        inMultiFight = false
        choiceFollowsFight = followsChoice
        lastResponse = ""
    }

    fun beginRound(nextAction: FightAction) {
        action = nextAction
        if (round == 0) round = 1
    }

    /**
     * Records only lifecycle facts. Detailed result and item processing stays
     * in AdventureManager/FightDomSync, so a response hook cannot double-apply
     * inventory or quest side effects.
     */
    fun recordResponse(html: String): Context {
        lastResponse = html
        val stillFighting = AdventureParser.isInMultiFight(html)
        inMultiFight = stillFighting
        if (round == 0) round = 1
        else if (stillFighting) round++
        if (html.contains("choice.php", ignoreCase = true)) {
            choiceFollowsFight = true
        }
        return context
    }

    fun clear() {
        round = 0
        action = null
        inMultiFight = false
        choiceFollowsFight = false
        lastResponse = ""
    }

    companion object {
        fun registerAction(
            action: FightAction,
            sessionLogger: SessionLogger?,
            preferences: Preferences?,
        ): Boolean = RequestLogger.registerRequest(
            "fight.php",
            sessionLogger = sessionLogger,
            preferences = preferences,
            formFields = action.formFields(),
        )
    }
}
