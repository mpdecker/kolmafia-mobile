package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.ash.AshRuntime
import net.sourceforge.kolmafia.ash.AshType
import net.sourceforge.kolmafia.ash.AshValue
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * Desktop [Macrofier] subset — filter override + CCS → KoL macrotext (Phases 1146–1175)
 * plus rave [macroCombo] expansion (Phases 1581–1595) + XLIII Track D consult depth
 * (Phases 6461–6470: hulking construct / RAM specials, semicolon raw-macro filters)
 * + XLIV Track A ASH filter callback execution (Phases 6491–6510).
 */
object Macrofier {
    /** Optional max MP for combo cost gate; [Int.MAX_VALUE] skips the early-out. */
    var maximumMp: Int = Int.MAX_VALUE

    /** Desktop [Macrofier] override stack top — raw macro or ASH filter name. */
    private var macroOverride: String? = null
    private var macroInterpreter: AshRuntime? = null

    /**
     * Desktop [FightRequest.combatFilterThatDidNothing] — reject identical filter returns
     * within the same round.
     */
    var combatFilterThatDidNothing: String? = null

    /** Desktop [Macrofier.setMacroOverride]. */
    fun setMacroOverride(filter: String?, runtime: AshRuntime?) {
        when {
            filter.isNullOrBlank() -> {
                macroOverride = null
                macroInterpreter = null
            }
            filter.contains(';') -> {
                macroOverride = filter
                macroInterpreter = null
            }
            else -> {
                macroOverride = filter
                macroInterpreter = runtime
            }
        }
        ChoiceCombatAshState.combatFilterOverride = filter?.takeIf { it.isNotBlank() }
    }

    /** Desktop [Macrofier.resetMacroOverride]. */
    fun resetMacroOverride() {
        macroOverride = null
        macroInterpreter = null
        ChoiceCombatAshState.combatFilterOverride = null
    }

    fun resetForTest() {
        resetMacroOverride()
        combatFilterThatDidNothing = null
        maximumMp = Int.MAX_VALUE
    }

    /**
     * Build fight.php macrotext for the current encounter.
     * Returns null when caller should fall back to zone pref / ASH combat script strings.
     */
    fun macrofy(
        monsterName: String = MonsterStatusTracker.getLastMonsterName(),
        preferences: Preferences? = null,
        filterOverride: String? = ChoiceCombatAshState.combatFilterOverride,
        maximumMp: Int = this.maximumMp,
    ): String? {
        val previousMp = this.maximumMp
        this.maximumMp = maximumMp
        try {
            return macrofyBody(monsterName, preferences, filterOverride)
        } finally {
            this.maximumMp = previousMp
        }
    }

    private fun macrofyBody(
        monsterName: String,
        preferences: Preferences?,
        filterOverride: String?,
    ): String? {
        // Desktop Macrofier: interpreter override executes ASH filter callback first.
        val override = filterOverride ?: macroOverride
        val interpreter = macroInterpreter
        if (interpreter != null && !override.isNullOrBlank() && !override.contains(';')) {
            executeAshFilter(interpreter, override)?.let { return it }
        }
        if (!override.isNullOrBlank()) {
            // Raw macro override (semicolon / known verb / quoted) — no interpreter
            if (interpreter == null) {
                normalizeFilterMacro(override)?.let { return it }
                // Bare name without interpreter → leave for CCS fallback
                return null
            }
            normalizeFilterMacro(override)?.let { return it }
        }

        val name = monsterName.ifBlank { MonsterStatusTracker.getLastMonsterName() }

        // Desktop Macrofier specials before CCS expansion
        if (name.equals("hulking construct", ignoreCase = true)) {
            return buildString {
                append("if hascombatitem 3146 && hascombatitem 3155\n")
                append("  use 3146,3155\n")
                append("endif\nrunaway; repeat\n")
            }
        }
        if (name.equals("rampaging adding machine", ignoreCase = true)) {
            // Desktop leaves RAM to non-macro combat (cannot safely macrofy)
            return null
        }

        if (preferences == null) return null
        if (!CombatActionManager.usingCustomCombat(preferences)) return null

        val encounter = name.ifBlank { "default" }
        val macro = StringBuilder()

        val thresh = preferences.getString("autoAbortThreshold", "0").toFloatOrNull() ?: 0f
        if (thresh > 0f) {
            macro.append("abort hppercentbelow ").append((thresh * 100).toInt()).append('\n')
        }

        macro.append("#mafiaheader\n")

        if (CombatActionManager.getStrategyLookup().getStrategy("global prefix") != null) {
            for (i in 0 until 1000) {
                val action = CombatActionManager.getCcsCombatAction("global prefix", i, true, preferences)
                if (!isSimpleAction(action)) break
                macroAction(macro, action, finalRound = false, preferences)
                if (CombatActionManager.atEndOfStrategy) break
            }
        }

        for (i in 0 until 1000) {
            val action = CombatActionManager.getCcsCombatAction(encounter, i, true, preferences)
            if (!isSimpleAction(action)) {
                // Consult / custom / delevel — stop macrofication so get_ccs_action / FightRequest
                // can consult the ASH/script action for this round (desktop Macrofier parity).
                if (i == 0) return null
                break
            }
            val atEnd = CombatActionManager.atEndOfStrategy
            if (atEnd) macro.append("mark mafiafinal\n")
            val before = macro.length
            macroAction(macro, action, finalRound = atEnd, preferences)
            if (atEnd) {
                if (before == macro.length) {
                    macro.append("call mafiaround; attack\n")
                }
                macro.append("goto mafiafinal")
                break
            }
        }
        return macro.toString().ifBlank { null }
    }

    /** Expand a single CCS/long action into KoL macro lines. */
    fun expandAction(action: String, preferences: Preferences? = null): String {
        val sb = StringBuilder()
        macroAction(sb, action, finalRound = false, preferences)
        return sb.toString().trimEnd()
    }

    /** Desktop [Macrofier.macroCombo]. */
    fun macroCombo(
        macro: StringBuilder,
        combo: IntArray,
        preferences: Preferences? = null,
        maximumMp: Int = this.maximumMp,
    ) {
        var cost = 0L
        for (skillId in combo) {
            cost += SkillDefinitionDatabase.getById(skillId)?.mpCost?.toLong() ?: 0L
        }
        if (cost > maximumMp) return

        val restore = preferences?.getBoolean("autoManaRestore", false) == true
        if (restore) {
            macro.append("while mpbelow ").append(cost).append('\n')
            macro.append("call mafiamp\nendwhile\n")
        } else {
            macro.append("if !mpbelow ").append(cost).append('\n')
        }
        macro.append("call mafiaround; ")
        for (skillId in combo) {
            macro.append("skill ").append(skillId).append("; ")
        }
        macro.append('\n')
        if (!restore) {
            macro.append("endif\n")
        }
    }

    /**
     * Desktop Macrofier ASH filter path: `execute(name, [round, monster, responseText])`.
     * Returns expanded macro / action string, or null to fall through to CCS.
     */
    private fun executeAshFilter(runtime: AshRuntime, functionName: String): String? {
        val round = ChoiceCombatAshState.currentRound.coerceAtLeast(0)
        val monsterName = MonsterStatusTracker.getLastMonsterName()
        val response = ChoiceCombatAshState.lastFightResponseText
        val args = listOf(
            AshValue.of(round.toLong()),
            AshValue(AshType.MONSTER, monsterName),
            AshValue.of(response),
        )
        val returnValue = try {
            runtime.executeUserFunction(functionName, args) ?: return null
        } catch (_: Exception) {
            return null
        }
        if (returnValue.type == AshType.VOID) return null
        val result = returnValue.toString()
        if (result.isEmpty()) return null
        if (result.startsWith("\"") && result.endsWith("\"") && result.length >= 2) {
            return buildString {
                append("#macro action\n")
                append(result.substring(1, result.length - 1))
                append('\n')
            }
        }
        if (result == combatFilterThatDidNothing) return "abort"
        combatFilterThatDidNothing = result
        return result
    }

    /**
     * Desktop [Macrofier.setMacroOverride] filter normalization:
     * - quoted string → `#macro action` body
     * - contains `;` or looks like KoL macrotext → raw macro
     * - otherwise → ASH consult function name (returns null so caller executes ASH)
     */
    fun normalizeFilterMacro(filter: String): String? {
        val t = filter.trim()
        if (t.isEmpty()) return null
        if (t.startsWith("\"") && t.endsWith("\"") && t.length >= 2) {
            return buildString {
                append("#macro action\n")
                append(t.substring(1, t.length - 1).trim())
                append('\n')
            }
        }
        // Semicolon / multi-line / known KoL verbs → raw macrotext
        if (t.contains(';') || t.contains('\n')) return t
        val lower = t.lowercase()
        if (lower.startsWith("abort") ||
            lower.startsWith("skill") ||
            lower.startsWith("attack") ||
            lower.startsWith("runaway") ||
            lower.startsWith("pickpocket") ||
            lower.startsWith("steal") ||
            lower.startsWith("use ") ||
            lower.startsWith("if ") ||
            lower.startsWith("while ") ||
            lower.startsWith("call ") ||
            lower.startsWith("mark ") ||
            lower.startsWith("goto ") ||
            lower.startsWith("#")
        ) {
            return t
        }
        // Bare identifier → ASH filter function name; Macrofier cannot expand it
        return null
    }

    private fun isSimpleAction(action: String): Boolean {
        if (CombatActionManager.isMacroAction(action)) return true
        val short = CombatActionManager.getShortCombatOptionName(action)
        if (short.startsWith("consult")) return false
        if (short == "custom") return false
        if (short == "delevel") return false
        if (short == "twiddle") return false
        return true
    }

    private fun macroAction(
        macro: StringBuilder,
        rawAction: String,
        finalRound: Boolean,
        preferences: Preferences?,
    ) {
        if (CombatActionManager.isMacroAction(rawAction)) {
            var line = rawAction.trim()
            if (line.startsWith("\"")) {
                line = line.removePrefix("\"").removeSuffix("\"").trim()
            }
            macro.append(line).append('\n')
            return
        }

        val action = CombatActionManager.getShortCombatOptionName(rawAction)
        when {
            action == "skip" -> return
            action == "special" -> macro.append("#special\n")
            action == "abort" -> {
                if (finalRound) {
                    macro.append("abort \"KoLmafia CCS abort\"\n")
                } else {
                    macro.append("abort \"Click Script button again to continue\"\n")
                    macro.append("#mafiarestart\n")
                }
            }
            action == "abort after" -> macro.append("abort \"Aborted by CCS request\"\n")
            action == "runaway" || action.startsWith("runaway") -> macro.append("runaway\n")
            action.startsWith("attack") -> macro.append("call mafiaround; attack\n")
            action == "steal" -> macro.append("pickpocket\n")
            action == "jiggle" -> macro.append("call mafiaround; jiggle\n")
            action.startsWith("skill") -> {
                val skill = action.removePrefix("skill").trim()
                if (skill.isNotEmpty()) {
                    macro.append("if hasskill ").append(skill).append('\n')
                    macro.append("  call mafiaround; skill ").append(skill).append('\n')
                    macro.append("endif\n")
                }
            }
            action.startsWith("combo ") -> {
                val name = action.substring(6)
                val combo = DiscoCombatHelper.getCombo(name)
                if (combo != null) {
                    val raveSteal = DiscoCombatHelper.COMBOS[DiscoCombatHelper.RAVE_STEAL][0]
                    val canonical = DiscoCombatHelper.disambiguateCombo(name)
                    if (!(canonical == raveSteal && !DiscoCombatHelper.canRaveSteal())) {
                        macroCombo(macro, combo, preferences)
                    }
                }
            }
            else -> {
                val item = action.trim()
                if (item.isNotEmpty() && item != "twiddle" && item != "delevel") {
                    macro.append("call mafiaround; use ").append(item).append('\n')
                }
            }
        }
    }
}
