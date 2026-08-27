package net.sourceforge.kolmafia.session

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
        lastChoice = 0
        lastDecision = 0
        lastChoiceResponseText = ""
        lastFightResponseText = ""
        lastFormFields.clear()
        combatFilterOverride = null
    }

    fun setFormFieldsFromPostData(postData: String) {
        lastFormFields.clear()
        postData.split("&").filter { it.isNotBlank() }.forEach { pair ->
            val eq = pair.indexOf('=')
            var name = if (eq >= 0) pair.substring(0, eq) else pair
            val value = if (eq >= 0) pair.substring(eq + 1) else ""
            while (lastFormFields.containsKey(name)) name = "${name}_"
            lastFormFields[name] = value
        }
    }

    fun noteChoiceVisit(choiceId: Int, responseText: String) {
        lastChoice = choiceId
        lastChoiceResponseText = responseText
        handlingChoice = true
        currentRound = 0
    }

    fun noteChoiceDecision(decision: Int, responseText: String? = null) {
        lastDecision = decision
        if (responseText != null) lastChoiceResponseText = responseText
    }

    fun noteFightStart(responseText: String) {
        lastFightResponseText = responseText
        currentRound = 1
        handlingChoice = false
    }

    fun noteFightRound(responseText: String) {
        lastFightResponseText = responseText
        if (currentRound < 1) currentRound = 1 else currentRound++
    }

    fun noteFightEnd(responseText: String = lastFightResponseText) {
        lastFightResponseText = responseText
        currentRound = 0
    }

    fun bufferOf(text: String): StringBuilder = StringBuilder(text)
}
