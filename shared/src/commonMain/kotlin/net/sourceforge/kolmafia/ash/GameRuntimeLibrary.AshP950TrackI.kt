package net.sourceforge.kolmafia.ash

import io.ktor.http.decodeURLQueryComponent
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

    // ── Phase 952 / 6481–6490 / 6591–6610: form_fields ─────────────
    regFn(scope, "form_fields", stringToString, emptyList()) { _, _ ->
        val result = AggregateValue(stringToString)
        val fields = ChoiceCombatAshState.lastFormFields
        if (fields.isNotEmpty()) {
            for ((k, v) in fields) {
                putFormField(result, k, v)
            }
            return@regFn result
        }
        // Desktop last-visit GET query decode (GenericRequest.decodeField).
        parseQueryFormFields(lastVisitPath, result)
        result
    }

    // ── Phase 953: choice_follows_fight ─────────────────────────────
    // XLIV/XLV: ChoiceCombatAshState post-fight flag (AdventureManager syncs from fight HTML)
    regFn(scope, "choice_follows_fight", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(ChoiceCombatAshState.choiceFollowsFight)
    }
}

/** Desktop GenericRequest.decodeField parity for form_fields query parsing. */
internal fun parseQueryFormFields(url: String, into: AggregateValue) {
    val q = url.indexOf('?')
    if (q < 0) return
    url.substring(q + 1).split('&').forEach { pair ->
        if (pair.isEmpty()) return@forEach
        val eq = pair.indexOf('=')
        val rawKey: String
        val rawValue: String
        if (eq < 0) {
            // Bare key with no '=' → empty value (desktop form field parity)
            rawKey = pair
            rawValue = ""
        } else if (eq == 0) {
            return@forEach
        } else {
            rawKey = pair.substring(0, eq)
            rawValue = pair.substring(eq + 1)
        }
        val key = rawKey.decodeURLQueryComponent()
        val value = rawValue.decodeURLQueryComponent()
        putFormField(into, key, value)
    }
}

/** Desktop duplicate-key policy: append `_` until the key is unique. */
internal fun putFormField(into: AggregateValue, key: String, value: String) {
    var unique = key
    while (into.map.containsKey(AshValue.of(unique))) {
        unique += "_"
    }
    into[AshValue.of(unique)] = AshValue.of(value)
}
