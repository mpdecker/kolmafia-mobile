package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [CustomCombatLookup] — parsed CCS file with section match (Phases 1131–1145).
 */
class CustomCombatLookup {
    private val childKeys = mutableListOf<CombatEncounterKey>()
    private val childLookup = linkedMapOf<String, CustomCombatStrategy>()

    fun getStrategy(encounterKey: String): CustomCombatStrategy? = childLookup[encounterKey]

    fun strategies(): Collection<CustomCombatStrategy> = childLookup.values

    fun clear() {
        childKeys.clear()
        childLookup.clear()
    }

    fun addEncounterKey(rawKey: String) {
        val key = CombatEncounterKey(rawKey)
        val encounterKey = key.toString()
        if (childLookup.containsKey(encounterKey)) {
            childLookup[encounterKey]?.removeAllChildren()
        } else {
            childKeys.add(key)
            childLookup[encounterKey] = CustomCombatStrategy(encounterKey)
        }
    }

    fun addEncounterAction(
        encounterKey: String,
        roundIndex: Int,
        indent: String,
        combatAction: String,
        isMacro: Boolean,
    ) {
        val strategy = childLookup[encounterKey] ?: return
        if (roundIndex < 0) {
            strategy.addCombatAction(strategy.getChildCount() + 1, indent, combatAction, isMacro)
        } else {
            strategy.addCombatAction(roundIndex, indent, combatAction, isMacro)
        }
    }

    fun clearEncounterKey(encounterKey: String) {
        for (strategy in childLookup.values) {
            if (strategy.name == encounterKey) {
                strategy.removeAllChildren()
            } else {
                strategy.resetActionCount()
            }
        }
    }

    /**
     * Desktop [CustomCombatLookup.getBestEncounterKey].
     * Optional [zoneForLocation] resolves adventure zone from [lastAdventure] location name.
     */
    fun getBestEncounterKey(
        encounter: String,
        preferences: Preferences? = null,
        zoneForLocation: ((String) -> String?)? = null,
    ): String {
        val encounterKey = CombatActionManager.encounterKey(encounter)
        val monster = MonsterDatabase.getByName(encounterKey)
        getLongestMatch(encounterKey, monster)?.let { return it }

        // Desktop: lastAdventure when monster known; else "unrecognized"
        val location = if (monster != null) {
            preferences?.getString("lastAdventure", "")
                ?.ifBlank { preferences.getString(Preferences.LAST_LOCATION, "") }
                ?.ifBlank { "unrecognized" }
                ?: "unrecognized"
        } else {
            "unrecognized"
        }
        getLongestMatch(location.lowercase(), monster)?.let { return it }

        val zone = zoneForLocation?.invoke(location)
        if (zone != null) {
            getLongestMatch(zone.lowercase(), monster)?.let { return it }
        }
        return "default"
    }

    private fun getLongestMatch(
        haystack: String,
        monster: net.sourceforge.kolmafia.data.MonsterDefinition?,
    ): String? {
        val phylum = monster?.phylum
        val element = monster?.defenseElement?.takeIf { it.isNotBlank() }
        val items = monster?.drops?.map { it.itemName }.orEmpty()
        var longestMatch: String? = null
        var longestMatchLength = 0
        for (childKey in childKeys) {
            if (childKey.matches(haystack, phylum, element, items)) {
                val childName = childKey.toString()
                if (childName.length > longestMatchLength) {
                    longestMatch = childName
                    longestMatchLength = childName.length
                }
            }
        }
        return longestMatch
    }

    fun load(text: String) {
        clear()
        addEncounterKey("default")
        val indent = StringBuilder()
        var encounterKey = "default"
        for ((lineNumber, raw) in text.lineSequence().withIndex()) {
            var line = raw.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("[")) {
                val strategy = getStrategy(encounterKey)
                if (strategy != null && strategy.getChildCount() == 0) {
                    strategy.addCombatAction(1, indent.toString(), "attack", false)
                }
                indent.clear()
                val close = line.lastIndexOf(']')
                encounterKey = if (close == -1) {
                    "ignore"
                } else {
                    CombatActionManager.encounterKey(line.substring(1, close).trim())
                }
                addEncounterKey(encounterKey)
                continue
            }

            if (CombatActionManager.isMacroAction(line)) {
                if (line.startsWith("\"")) {
                    line = line.substring(1).trim()
                    if (line.isEmpty()) continue
                    if (line.endsWith("\"")) line = line.dropLast(1)
                }
                if (line.startsWith("end") && indent.isNotEmpty()) {
                    indent.deleteRange(0, minOf(4, indent.length))
                }
                addEncounterAction(encounterKey, -1, indent.toString(), line, true)
                if (line.startsWith("if") || line.startsWith("while") || line.startsWith("sub")) {
                    indent.append("    ")
                }
                continue
            }

            var roundIndex = -1
            if (line.first().isDigit()) {
                val colonIndex = line.indexOf(':')
                if (colonIndex != -1) {
                    roundIndex = line.substring(0, colonIndex).toIntOrNull() ?: -1
                    line = line.substring(colonIndex + 1).trim()
                }
            }
            addEncounterAction(encounterKey, roundIndex, indent.toString(), line, false)
        }
        val strategy = getStrategy(encounterKey)
        if (strategy != null && strategy.getChildCount() == 0) {
            strategy.addCombatAction(1, indent.toString(), "attack", false)
        }
    }

    fun store(): String = buildString {
        for (strategy in childLookup.values) {
            append(strategy.store())
        }
    }
}
