package net.sourceforge.kolmafia.session

import io.ktor.http.decodeURLQueryComponent

/**
 * Session state backing combat/choice ASH APIs (desktop ChoiceManager / FightRequest fields).
 * Updated from [net.sourceforge.kolmafia.adventure.AdventureManager] and readable without HTTP.
 */
object ChoiceCombatAshState {
    var currentRound: Int = 0
    var handlingChoice: Boolean = false
    var choiceFollowsFight: Boolean = false
    var fightFollowsChoice: Boolean = false
    var inMultiFight: Boolean = false
    /** Desktop [ChoiceManager.canWalkAway] — refreshed on each choice visit. */
    var canWalkAway: Boolean = true
    var lastChoice: Int = 0
    var lastDecision: Int = 0
    var lastChoiceResponseText: String = ""
    var lastFightResponseText: String = ""
    /** Last relay-style form fields for [form_fields] ASH (name → value). */
    var lastFormFields: MutableMap<String, String> = linkedMapOf()
    /** Optional combat filter override from [run_combat] (Macrofier parity — live). */
    var combatFilterOverride: String? = null

    fun reset() {
        currentRound = 0
        handlingChoice = false
        choiceFollowsFight = false
        fightFollowsChoice = false
        inMultiFight = false
        canWalkAway = true
        lastChoice = 0
        lastDecision = 0
        lastChoiceResponseText = ""
        lastFightResponseText = ""
        lastFormFields.clear()
        combatFilterOverride = null
        AvailableCombatSkills.clear()
        FightRamTracker.reset()
    }

    fun setFormFieldsFromPostData(postData: String) {
        lastFormFields.clear()
        postData.split("&").filter { it.isNotBlank() }.forEach { pair ->
            val eq = pair.indexOf('=')
            val rawName = if (eq >= 0) pair.substring(0, eq) else pair
            val rawValue = if (eq >= 0) pair.substring(eq + 1) else ""
            // Desktop GenericRequest.decodeField parity for form_fields ASH.
            var name = decodeFormField(rawName)
            val value = decodeFormField(rawValue)
            while (lastFormFields.containsKey(name)) name = "${name}_"
            lastFormFields[name] = value
        }
    }

    /** Desktop [GenericRequest.decodeField] — URLDecoder UTF-8 (+ → space, %XX). */
    private fun decodeFormField(raw: String): String =
        if (raw.isEmpty()) raw else raw.decodeURLQueryComponent()


    fun noteChoiceVisit(choiceId: Int, responseText: String) {
        lastChoice = choiceId
        lastChoiceResponseText = responseText
        handlingChoice = true
        currentRound = 0
        // Desktop ChoiceManager.setCanWalkAway(ChoiceControl.canWalkFromChoice(choice))
        canWalkAway = if (choiceId <= 0) {
            true
        } else {
            net.sourceforge.kolmafia.adventure.choice.ChoiceWalkAway.canWalkFromChoice(choiceId)
        }
    }

    fun noteChoiceDecision(decision: Int, responseText: String? = null) {
        lastDecision = decision
        if (responseText != null) {
            lastChoiceResponseText = responseText
            // Desktop ChoiceManager → fight.php redirect / combat HTML after choice
            if (responseText.contains("fight.php", ignoreCase = true) ||
                responseText.contains("You're fighting", ignoreCase = true) ||
                responseText.contains("Monster name", ignoreCase = true)
            ) {
                fightFollowsChoice = true
                handlingChoice = false
            }
        }
    }

    fun noteFightStart(responseText: String) {
        lastFightResponseText = responseText
        currentRound = 1
        if (handlingChoice) {
            fightFollowsChoice = true
        }
        handlingChoice = false
        choiceFollowsFight = false
    }

    fun noteFightRound(responseText: String) {
        lastFightResponseText = responseText
        if (currentRound < 1) currentRound = 1 else currentRound++
        // Desktop FightRequest.choiceFollowsFight via FIGHTCHOICE / choice.php link
        if (responseText.contains("choice.php", ignoreCase = true)) {
            choiceFollowsFight = true
        }
    }

    fun noteFightEnd(responseText: String = lastFightResponseText) {
        lastFightResponseText = responseText
        currentRound = 0
        if (responseText.contains("choice.php", ignoreCase = true)) {
            choiceFollowsFight = true
        }
        AvailableCombatSkills.clear()
        FightRamTracker.onFightEnd()
    }

    fun bufferOf(text: String): StringBuilder = StringBuilder(text)
}
