package net.sourceforge.kolmafia.session

import kotlinx.coroutines.coroutineScope
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TavernCellarSync

/**
 * Desktop [TavernManager] headless port — cellar explore / faucet / baron goals.
 * Phases 5291–5310 (Behavioral Deepen XXIV).
 */
object TavernManager {

    const val EXPLORE = 1
    const val FAUCET = 2
    const val BARON = 3
    const val FIGHT_BARON = 4

    /** Desktop searchOrder (1-based square indices). */
    private val SEARCH_ORDER = intArrayOf(
        4, 3, 2, 1, 6, 11, 16, 21, 22, 17, 23, 24, 25, 12, 7, 8, 13, 18, 19, 14, 9, 10, 15, 20,
    )

    /** When ≥ 0, [recommendSquare] / adventure form uses this 0-based square. */
    var overrideSquare: Int = -1

    data class ExploreDeps(
        val preferences: Preferences,
        val characterState: CharacterState?,
        val questDatabase: QuestDatabase?,
        val adventureManager: AdventureManager?,
        val visitUrl: suspend (String) -> String?,
        val cellarLocation: () -> AdventureLocation? = {
            AdventureDatabase.getByName("The Typical Tavern Cellar")?.toLocation()
        },
    )

    fun faucetSquare(layout: String): Int {
        val idx = layout.indexOf('3')
        return if (idx < 0) 0 else idx + 1
    }

    fun baronSquare(layout: String): Int {
        val idx = layout.indexOf('4')
        return if (idx < 0) 0 else idx + 1
    }

    fun mansionSquare(layout: String): Int {
        val idx = layout.indexOf('6')
        return if (idx < 0) 0 else idx + 1
    }

    fun nextUnexploredSquare(layout: String): Int {
        if (!layout.contains('0')) return 0
        for (square in SEARCH_ORDER) {
            val idx = square - 1
            if (idx in layout.indices && layout[idx] == '0') return square
        }
        val first = layout.indexOf('0')
        return if (first < 0) 0 else first + 1
    }

    fun recommendSquare(layout: String, level: Int): Int {
        if (overrideSquare >= 0) return overrideSquare + 1
        if (level < 3) return 0
        val faucet = faucetSquare(layout)
        if (faucet > 0) return faucet
        return nextUnexploredSquare(layout)
    }

    fun locateTavernFaucet(deps: ExploreDeps): Int =
        kotlinx.coroutines.runBlocking { exploreTavern(FAUCET, deps) }

    fun locateBaron(deps: ExploreDeps): Int =
        kotlinx.coroutines.runBlocking { exploreTavern(BARON, deps) }

    fun fightBaron(deps: ExploreDeps): Int =
        kotlinx.coroutines.runBlocking { exploreTavern(FIGHT_BARON, deps) }

    fun exploreAll(deps: ExploreDeps): Int =
        kotlinx.coroutines.runBlocking { exploreTavern(EXPLORE, deps) }

    suspend fun exploreTavern(goal: Int, deps: ExploreDeps): Int {
        val state = deps.characterState
        val ascension = state?.ascensionNumber ?: 0
        // Prefer recorded layout (including short test/legacy strings) before cellar sync wipe.
        var layout = recordedLayout(deps.preferences, ascension)

        fun faucet() = layout.indexOf('3')
        fun baron() = layout.indexOf('4')
        fun mansion() = layout.indexOf('6')
        fun unexplored() = layout.indexOf('0')

        // Known-goal short-circuit before level gate (layout already recorded).
        if (goal == FAUCET && faucet() >= 0) return faucet() + 1
        if (goal == BARON && baron() >= 0) return baron() + 1
        if ((goal == BARON || goal == FIGHT_BARON) && mansion() >= 0) return mansion() + 1
        if (goal == EXPLORE && layout.length >= 25 && unexplored() < 0) return 0

        val level = state?.level ?: 0
        val exploathing = state?.isKingdomOfExploathing == true
        if (level < 3 && !exploathing) return -1

        deps.visitUrl("council.php")
        deps.visitUrl("tavern.php?place=barkeep")
        deps.visitUrl("cellar.php")
        layout = TavernCellarSync.tavernLayout(deps.preferences, ascension)

        if (goal == FAUCET && faucet() >= 0) return faucet() + 1
        if (goal == BARON && baron() >= 0) return baron() + 1
        if ((goal == BARON || goal == FIGHT_BARON) && mansion() >= 0) return mansion() + 1
        if (goal == EXPLORE && unexplored() < 0) return 0

        val adventureManager = deps.adventureManager ?: return -1
        val location = deps.cellarLocation() ?: return -1

        val oldBaron = deps.preferences.getInt("choiceAdventure511", 0)
        if (oldBaron != 2) deps.preferences.setInt("choiceAdventure511", 2)
        deps.preferences.setInt("choiceAdventure509", 1)
        deps.preferences.setInt("choiceAdventure1000", 1)

        var hadFaucet = faucet() >= 0
        var turns = 0
        val maxTurns = 30
        while (
            turns < maxTurns &&
            (state?.currentHp ?: 1) > 0 &&
            (state?.adventuresLeft ?: 0) > 0 &&
            (
                (goal == FAUCET && faucet() < 0) ||
                    (goal == BARON && baron() < 0) ||
                    (goal == FIGHT_BARON && baron() < 0) ||
                    (goal == EXPLORE && unexplored() >= 0)
                )
        ) {
            val next = nextUnexploredSquare(layout)
            if (next <= 0) break
            overrideSquare = next - 1
            try {
                coroutineScope {
                    adventureManager.runAdventures(location, 1, this).join()
                }
            } finally {
                overrideSquare = -1
            }
            layout = TavernCellarSync.tavernLayout(deps.preferences, ascension)
            if (!hadFaucet && faucet() >= 0) {
                deps.visitUrl("tavern.php?place=barkeep")
                deps.questDatabase?.setQuestIfBetter(Quest.RAT, "step2")
                hadFaucet = true
            }
            turns++
        }

        if (oldBaron != 2) deps.preferences.setInt("choiceAdventure511", oldBaron)

        return when (goal) {
            FAUCET -> if (faucet() >= 0) faucet() + 1 else -1
            BARON -> if (baron() >= 0) baron() + 1 else if (mansion() >= 0) mansion() + 1 else -1
            FIGHT_BARON -> {
                val b = baron()
                if (b < 0) return -1
                if (oldBaron != 1) deps.preferences.setInt("choiceAdventure511", 1)
                overrideSquare = b
                try {
                    coroutineScope {
                        adventureManager.runAdventures(location, 1, this).join()
                    }
                } finally {
                    overrideSquare = -1
                    if (oldBaron != 1) deps.preferences.setInt("choiceAdventure511", oldBaron)
                }
                b + 1
            }
            EXPLORE -> if (unexplored() < 0) 0 else unexplored() + 1
            else -> -1
        }
    }

    /** Desktop-compatible layout read that preserves short recorded strings for goal lookup. */
    private fun recordedLayout(preferences: Preferences, ascensionNumber: Int): String {
        val raw = preferences.getString("tavernLayout", "")
        if (raw.length == 25) return raw
        if (raw.isNotBlank()) return raw.padEnd(25, '0').take(25)
        return TavernCellarSync.tavernLayout(preferences, ascensionNumber)
    }
}
