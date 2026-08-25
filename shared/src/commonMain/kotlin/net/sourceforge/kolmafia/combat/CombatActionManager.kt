package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [CombatActionManager] — CCS load/resolve (Phases 1131–1175).
 */
object CombatActionManager {
    private val strategyLookup = CustomCombatLookup()
    private val availableLookups = linkedSetOf("default")
    var atEndOfStrategy: Boolean = false
        private set

    fun getStrategyLookup(): CustomCombatLookup = strategyLookup

    fun getStrategyLookupName(preferences: Preferences?): String {
        val script = preferences?.getString("customCombatScript", "").orEmpty()
        return script.ifBlank { "default" }
    }

    fun getAvailableLookups(): Set<String> = availableLookups.toSet()

    fun encounterKey(line: String, changeCase: Boolean = true): String {
        var trimmed = line.trim().replace(Regex(" {2,}"), " ")
        val key = trimmed.lowercase()
        when {
            key.startsWith("a ") -> {
                trimmed = trimmed.substring(2)
            }
            key.startsWith("an ") -> {
                trimmed = trimmed.substring(3)
            }
            key.startsWith("the ") -> Unit
            key.startsWith("some ") -> {
                trimmed = trimmed.substring(5)
            }
        }
        return if (changeCase) trimmed.lowercase() else trimmed
    }

    fun isMacroAction(action: String): Boolean {
        val a = action.trimStart()
        return a.startsWith("scrollwhendone") ||
            a.startsWith("mark ") ||
            a.startsWith("goto ") ||
            a.startsWith("if ") ||
            a.startsWith("endif") ||
            a.startsWith("while ") ||
            a.startsWith("endwhile") ||
            a.startsWith("sub ") ||
            a.startsWith("endsub") ||
            a.startsWith("call ") ||
            a.startsWith("#") ||
            a.startsWith("\"")
    }

    fun getLongCombatOptionName(action: String?): String {
        if (action == null) return "attack with weapon"
        val trimmed = action.trim()
        if (trimmed.startsWith("attack") || trimmed.isEmpty()) return "attack with weapon"
        if (isMacroAction(trimmed)) return trimmed
        if (trimmed.contains("pick") ||
            (trimmed.contains("steal") &&
                !trimmed.contains("stealth") &&
                !trimmed.contains("combo") &&
                !trimmed.contains("accordion") &&
                !trimmed.contains("heart"))
        ) {
            return "try to steal an item"
        }
        if (trimmed.equals("default", ignoreCase = true)) return "default"
        if (trimmed.startsWith("section", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("jiggle", ignoreCase = true)) return "jiggle chefstaff"
        if (trimmed.startsWith("special", ignoreCase = true)) return "special action"
        if (trimmed.equals("skip", ignoreCase = true)) return "skip"
        if (trimmed.equals("stun", ignoreCase = true)) return "stun"
        if (trimmed.startsWith("note", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("abort", ignoreCase = true)) {
            return if (trimmed.contains("after")) "abort after this combat" else "abort"
        }
        if (trimmed.startsWith("consult", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("custom", ignoreCase = true)) return "custom combat script"
        if (trimmed.startsWith("delevel", ignoreCase = true)) return "delevel and plink"
        if (trimmed.startsWith("twiddle", ignoreCase = true)) return "twiddle your thumbs"
        if (trimmed.contains("run") && trimmed.contains("away")) {
            val chance = Regex("""(\d+)\s*%""").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return if (chance <= 0) "try to run away"
            else "run away if $chance% chance of being free"
        }
        if (trimmed.startsWith("combo ", ignoreCase = true)) {
            val combo = DiscoCombatHelper.disambiguateCombo(trimmed.substring(6))
                ?: return "note unknown $trimmed"
            return "combo $combo"
        }
        if (trimmed.startsWith("item", ignoreCase = true) || trimmed.startsWith("use ", ignoreCase = true)) {
            val rest = if (trimmed.startsWith("use ", ignoreCase = true)) trimmed.substring(4).trim()
            else trimmed.substring(4).trim()
            return if (rest.startsWith("attack")) rest else "item $rest"
        }
        if (trimmed.startsWith("skill", ignoreCase = true)) {
            return "skill ${trimmed.substring(5).trim().lowercase()}"
        }
        // Unknown token — leave as skill-ish or item-ish passthrough
        return trimmed
    }

    fun getShortCombatOptionName(action: String?): String {
        if (action == null) return "attack"
        if (action.startsWith("consult")) return action
        if (action.equals("default", ignoreCase = true)) return "default"
        val trimmed = action.trim()
        if (isMacroAction(trimmed)) return trimmed
        if (trimmed.all { it.isDigit() }) return trimmed
        if (trimmed.startsWith("attack") || trimmed.isEmpty()) return "attack"
        if (trimmed.startsWith("abort")) {
            return if (trimmed.contains("after")) "abort after" else "abort"
        }
        if (trimmed.contains("pick") ||
            (trimmed.contains("steal") &&
                !trimmed.contains("stealth") &&
                !trimmed.contains("combo") &&
                !trimmed.contains("accordion") &&
                !trimmed.contains("heart"))
        ) {
            return "steal"
        }
        if (trimmed.startsWith("jiggle")) return "jiggle"
        if (trimmed.startsWith("special")) return "special"
        if (trimmed.equals("skip") || trimmed.startsWith("note")) return "skip"
        if (trimmed.startsWith("custom")) return "custom"
        if (trimmed.startsWith("delevel")) return "delevel"
        if (trimmed.startsWith("twiddle")) return "twiddle"
        if (trimmed.contains("run") && trimmed.contains("away")) {
            val chance = Regex("""(\d+)\s*%""").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return if (chance <= 0) "runaway" else "runaway$chance"
        }
        if (trimmed.startsWith("combo ")) {
            val name = trimmed.substring(6)
            val combo = DiscoCombatHelper.disambiguateCombo(name)
                ?: return "skip"
            return "combo $combo"
        }
        if (trimmed.startsWith("skill")) {
            return "skill ${trimmed.substring(5).trim().lowercase()}"
        }
        if (trimmed.startsWith("item") || trimmed.startsWith("use ")) {
            val rest = if (trimmed.startsWith("use ")) trimmed.substring(4).trim()
            else trimmed.substring(4).trim()
            return rest
        }
        return trimmed
    }

    fun usingCustomCombat(preferences: Preferences?): Boolean {
        val battle = preferences?.getString("battleAction", "").orEmpty()
        if (battle.startsWith("custom", ignoreCase = true)) return true
        // Mobile: customCombatScript alone activates CCS for adventure-loop glue
        return preferences?.getString("customCombatScript", "").orEmpty().isNotBlank() &&
            strategyLookup.strategies().any { it.getChildCount() > 0 }
    }

    fun loadStrategyLookup(name: String?, preferences: Preferences?): Boolean {
        var n = name?.trim().orEmpty().ifBlank { "default" }
        if (n.endsWith(".ccs", ignoreCase = true)) n = n.dropLast(4)
        availableLookups.add(n)

        val path = "ccs/$n.ccs"
        var text = UserDataFileIO.readText(path)
        if (text == null) {
            text = "[ default ]\nspecial action\nattack with weapon\n"
            try {
                UserDataFileIO.writeText(path, text)
            } catch (_: Exception) {
                // best-effort seed
            }
        }
        strategyLookup.load(text)
        preferences?.setString("customCombatScript", n)
        if (preferences?.getString("battleAction", "").orEmpty().isBlank()) {
            preferences?.setString("battleAction", "custom combat script")
        }
        return true
    }

    fun loadFromText(text: String, name: String = "default", preferences: Preferences? = null) {
        availableLookups.add(name)
        strategyLookup.load(text)
        preferences?.setString("customCombatScript", name)
    }

    fun getBestEncounterKey(encounter: String, preferences: Preferences?): String =
        strategyLookup.getBestEncounterKey(
            encounter,
            preferences,
            zoneForLocation = { location ->
                AdventureDatabase.getByName(location)?.zoneName
            },
        )

    fun getCombatAction(
        encounter: String,
        roundIndex: Int,
        allowMacro: Boolean,
        preferences: Preferences?,
    ): String {
        atEndOfStrategy = false
        if (roundIndex < 0 || roundIndex >= 100) {
            atEndOfStrategy = true
            return "abort"
        }

        if (encounter != "global prefix" && !usingCustomCombat(preferences)) {
            val action = preferences?.getString("battleAction", "").orEmpty()
            // Non-custom: auto-steal / special prefix then battleAction (desktop rounds 0–3)
            return when (roundIndex) {
                0 -> if (preferences?.getBoolean("autoSteal") == true) "try to steal an item" else "skip"
                1 -> "skip"
                2 -> "skip"
                3 -> "special action"
                else -> {
                    atEndOfStrategy = true
                    action.ifBlank { "attack with weapon" }
                }
            }
        }

        return getCcsCombatAction(encounter, roundIndex, allowMacro, preferences)
    }

    /** Always resolve from loaded CCS sections (ignores non-custom battleAction prefix rounds). */
    fun getCcsCombatAction(
        encounter: String,
        roundIndex: Int,
        allowMacro: Boolean,
        preferences: Preferences?,
    ): String {
        atEndOfStrategy = false
        if (roundIndex < 0 || roundIndex >= 100) {
            atEndOfStrategy = true
            return "abort"
        }
        val encounterKey = getBestEncounterKey(encounter, preferences)
        val strategy = strategyLookup.getStrategy(encounterKey)
            ?: strategyLookup.getStrategy("default")
            ?: return "attack with weapon"
        val actionCount = strategy.getActionCount(strategyLookup)
        if (roundIndex + 1 >= actionCount) {
            atEndOfStrategy = true
        }
        return strategy.getAction(strategyLookup, roundIndex, allowMacro)
    }

    /** Test hook — clear loaded CCS. */
    fun resetForTest() {
        strategyLookup.clear()
        strategyLookup.addEncounterKey("default")
        availableLookups.clear()
        availableLookups.add("default")
        atEndOfStrategy = false
    }
}
