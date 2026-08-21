package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * AshP950–955 Track I — Choice/combat deepen.
 *
 * Phase 950: available_choice_text_inputs
 * Phase 951: available_choice_select_inputs
 * Phase 952: form_fields
 * Phase 953: choice_follows_fight (ChoiceCombatAshState-aware)
 * Phase 954: spoiler honor regression anchor (wired in AshP891)
 * Phase 955: run_choice custom / run_combat filter honor regression
 */
internal fun GameRuntimeLibrary.registerAshP950TrackIBatch(scope: AshScope) {
    val stringToString = AggregateType(AshType.STRING, AshType.STRING)
    val selectMapType = AggregateType(AshType.STRING, stringToString)

    // ── Phase 950: available_choice_text_inputs ─────────────────────
    regFn(
        scope,
        "available_choice_text_inputs",
        stringToString,
        listOf("decision" to AshType.INT),
    ) { _, args ->
        val result = AggregateValue(stringToString)
        val html = ChoiceCombatAshState.lastChoiceResponseText
        if (html.isBlank()) return@regFn result
        val decision = args[0].toLong().toInt()
        val names = ChoiceUtilities.parseTextInputs(html)[decision].orEmpty()
        for (name in names) {
            result[AshValue.of(name)] = AshValue.of("")
        }
        result
    }

    // ── Phase 951: available_choice_select_inputs ───────────────────
    regFn(
        scope,
        "available_choice_select_inputs",
        selectMapType,
        listOf("decision" to AshType.INT),
    ) { _, args ->
        val result = AggregateValue(selectMapType)
        val html = ChoiceCombatAshState.lastChoiceResponseText
        if (html.isBlank()) return@regFn result
        val decision = args[0].toLong().toInt()
        val selects = ChoiceUtilities.parseSelectInputsWithTags(html)[decision].orEmpty()
        for ((name, options) in selects) {
            val inner = AggregateValue(stringToString)
            for ((value, label) in options) {
                inner[AshValue.of(value)] = AshValue.of(label)
            }
            result[AshValue.of(name)] = inner
        }
        result
    }

    // ── Phase 952: form_fields ──────────────────────────────────────
    regFn(scope, "form_fields", stringToString, emptyList()) { _, _ ->
        val result = AggregateValue(stringToString)
        for ((k, v) in ChoiceCombatAshState.lastFormFields) {
            result[AshValue.of(k)] = AshValue.of(v)
        }
        result
    }

    // ── Phase 953: choice_follows_fight ─────────────────────────────
    regFn(scope, "choice_follows_fight", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(ChoiceCombatAshState.choiceFollowsFight)
    }
}
