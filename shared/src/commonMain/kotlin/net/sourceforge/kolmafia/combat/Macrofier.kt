package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * Desktop [Macrofier] subset — filter override + CCS → KoL macrotext (Phases 1146–1175)
 * plus rave [macroCombo] expansion (Phases 1581–1595).
 */
object Macrofier {
    /** Optional max MP for combo cost gate; [Int.MAX_VALUE] skips the early-out. */
    var maximumMp: Int = Int.MAX_VALUE

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
        if (!filterOverride.isNullOrBlank()) {
            return normalizeFilterMacro(filterOverride)
        }
        if (preferences == null) return null
        if (!CombatActionManager.usingCustomCombat(preferences)) return null

        val name = monsterName.ifBlank { "default" }
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
            val action = CombatActionManager.getCcsCombatAction(name, i, true, preferences)
            if (!isSimpleAction(action)) {
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

    private fun normalizeFilterMacro(filter: String): String {
        val t = filter.trim()
        if (t.startsWith("\"") && t.endsWith("\"") && t.length >= 2) {
            return buildString {
                append("#macro action\n")
                append(t.substring(1, t.length - 1).trim())
                append('\n')
            }
        }
        return t
    }

    private fun isSimpleAction(action: String): Boolean {
        if (CombatActionManager.isMacroAction(action)) return true
        val short = CombatActionManager.getShortCombatOptionName(action)
        if (short.startsWith("consult")) return false
        if (short == "custom") return false
        if (short == "delevel") return false
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
