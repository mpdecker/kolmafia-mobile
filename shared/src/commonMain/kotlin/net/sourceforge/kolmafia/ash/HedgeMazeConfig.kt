package net.sourceforge.kolmafia.ash

import kotlin.math.ceil
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

internal enum class HedgeMazeMode {
    TRAPS,
    GOPHER_DUCK,
    CHIHUAHUA_KIWI,
    NUGGLETS,
}

internal fun hedgeMazeModeFromTag(tag: String): HedgeMazeMode? = when (tag.lowercase()) {
    "traps" -> HedgeMazeMode.TRAPS
    "gopher", "duck" -> HedgeMazeMode.GOPHER_DUCK
    "chihuahua", "kiwi" -> HedgeMazeMode.CHIHUAHUA_KIWI
    "nugglets" -> HedgeMazeMode.NUGGLETS
    else -> null
}

internal fun hedgeMazeErrorMessage(status: String, questDatabase: QuestDatabase): String = when {
    status == QuestDatabase.UNSTARTED ->
        "You haven't been given the quest to fight the Sorceress!"
    questDatabase.isQuestLaterThan(Quest.FINAL, "step4") ->
        "You have already completed the Hedge Maze."
    else ->
        "You haven't reached the Hedge Maze yet."
}

internal fun GameRuntimeLibrary.applyHedgeMazeMode(mode: HedgeMazeMode): Boolean {
    val db = questDatabase ?: return false
    if (db.getProgress(Quest.FINAL) != "step4") return false
    val prefs = preferences ?: return false
    when (mode) {
        HedgeMazeMode.TRAPS -> {
            prefs.setInt("choiceAdventure1005", 2)
            prefs.setInt("choiceAdventure1008", 2)
            prefs.setInt("choiceAdventure1011", 2)
            prefs.setInt("choiceAdventure1013", 1)
            prefs.setInt("choiceAdventure1006", 1)
            prefs.setInt("choiceAdventure1007", 1)
            prefs.setInt("choiceAdventure1009", 1)
            prefs.setInt("choiceAdventure1010", 1)
            prefs.setInt("choiceAdventure1012", 1)
        }
        HedgeMazeMode.GOPHER_DUCK -> {
            prefs.setInt("choiceAdventure1005", 1)
            prefs.setInt("choiceAdventure1006", 2)
            prefs.setInt("choiceAdventure1008", 1)
            prefs.setInt("choiceAdventure1009", 2)
            prefs.setInt("choiceAdventure1011", 1)
            prefs.setInt("choiceAdventure1012", 1)
            prefs.setInt("choiceAdventure1013", 1)
            prefs.setInt("choiceAdventure1007", 1)
            prefs.setInt("choiceAdventure1010", 1)
        }
        HedgeMazeMode.CHIHUAHUA_KIWI -> {
            prefs.setInt("choiceAdventure1005", 1)
            prefs.setInt("choiceAdventure1006", 1)
            prefs.setInt("choiceAdventure1007", 2)
            prefs.setInt("choiceAdventure1009", 1)
            prefs.setInt("choiceAdventure1010", 2)
            prefs.setInt("choiceAdventure1012", 1)
            prefs.setInt("choiceAdventure1013", 1)
            prefs.setInt("choiceAdventure1008", 1)
            prefs.setInt("choiceAdventure1011", 1)
        }
        HedgeMazeMode.NUGGLETS -> {
            for (room in 1005..1013) {
                prefs.setInt("choiceAdventure$room", 1)
            }
        }
    }
    return true
}

internal fun estimateMazeTurns(prefs: Preferences, currentRoom: Int): Int {
    var turns = 0
    var room = 1005 + if (currentRoom in 1..9) currentRoom - 1 else 0
    while (room <= 1013) {
        turns++
        if (prefs.getInt("choiceAdventure$room", 0) == 1) {
            room++
            continue
        }
        room += when (room) {
            1005 -> 3
            1006 -> 2
            1007 -> 2
            1008 -> 3
            1009 -> 2
            1010 -> 2
            1011 -> 2
            else -> 1
        }
    }
    return turns
}

internal fun estimateTrapHpLost(
    prefs: Preferences,
    currentRoom: Int,
    maxHp: Int,
    modifiers: CurrentModifiers,
): Int {
    var hpLost = 0
    var trap1 = elementFromPref(prefs.getString("nsChallenge3", "none"))
    var trap2 = elementFromPref(prefs.getString("nsChallenge4", "none"))
    var trap3 = elementFromPref(prefs.getString("nsChallenge5", "none"))

    if (currentRoom <= 1) {
        if (trap1 == null) trap1 = lowestResistElement(modifiers, emptySet())
        hpLost += trapDamage(maxHp, 0.9, resistPercent(modifiers, trap1))
    }
    if (currentRoom <= 4) {
        if (trap2 == null) trap2 = lowestResistElement(modifiers, setOfNotNull(trap1))
        hpLost += trapDamage(maxHp, 0.8, resistPercent(modifiers, trap2))
    }
    if (currentRoom <= 7) {
        if (trap3 == null) trap3 = lowestResistElement(modifiers, setOfNotNull(trap1, trap2))
        hpLost += trapDamage(maxHp, 0.7, resistPercent(modifiers, trap3))
    }
    return hpLost
}

private enum class HedgeElement { HOT, COLD, SPOOKY, STENCH, SLEAZE }

private fun elementFromPref(value: String): HedgeElement? = when (value.lowercase()) {
    "hot" -> HedgeElement.HOT
    "cold" -> HedgeElement.COLD
    "spooky" -> HedgeElement.SPOOKY
    "stench" -> HedgeElement.STENCH
    "sleaze" -> HedgeElement.SLEAZE
    else -> null
}

private fun resistPercent(modifiers: CurrentModifiers, element: HedgeElement): Double =
    when (element) {
        HedgeElement.HOT -> modifiers.values.get(DoubleModifier.HOT_RESISTANCE)
        HedgeElement.COLD -> modifiers.values.get(DoubleModifier.COLD_RESISTANCE)
        HedgeElement.SPOOKY -> modifiers.values.get(DoubleModifier.SPOOKY_RESISTANCE)
        HedgeElement.STENCH -> modifiers.values.get(DoubleModifier.STENCH_RESISTANCE)
        HedgeElement.SLEAZE -> modifiers.values.get(DoubleModifier.SLEAZE_RESISTANCE)
    }

private fun lowestResistElement(
    modifiers: CurrentModifiers,
    exclude: Set<HedgeElement>,
): HedgeElement {
    val candidates = HedgeElement.entries.filter { it !in exclude }
    return candidates.minByOrNull { resistPercent(modifiers, it) } ?: HedgeElement.COLD
}

private fun trapDamage(maxHp: Int, fraction: Double, resistPct: Double): Int =
    ceil(maxHp * fraction * (1.0 - resistPct / 100.0)).toInt()

internal fun wouldSurviveTraps(
    state: CharacterState,
    prefs: Preferences,
    modifiers: CurrentModifiers,
    inDarkGyffte: Boolean,
): Boolean {
    val hpLost = estimateTrapHpLost(
        prefs,
        prefs.getInt("currentHedgeMazeRoom", 0),
        state.maxHp,
        modifiers,
    )
    return when {
        state.maxHp <= 0 -> true
        hpLost >= state.maxHp -> false
        inDarkGyffte && hpLost >= state.currentHp -> false
        else -> true
    }
}
