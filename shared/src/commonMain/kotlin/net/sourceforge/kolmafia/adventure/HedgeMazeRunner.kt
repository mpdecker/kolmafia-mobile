package net.sourceforge.kolmafia.adventure

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.ash.HedgeMazeMode
import net.sourceforge.kolmafia.ash.estimateMazeTurns
import net.sourceforge.kolmafia.ash.wouldSurviveTraps
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

internal class HedgeMazeRunner(
    private val httpClient: HttpClient,
    private val adventureManager: AdventureManager,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val questDatabase: QuestDatabase,
    private val recoveryManager: RecoveryManager?,
    private val inventoryManager: InventoryManager?,
    private val skillManager: SkillManager?,
    private val effectManager: EffectManager?,
    private val processQuestHooks: (html: String, url: String) -> Unit,
) {
    private val mazeLocation = AdventureLocation("nstower", "The Hedge Maze", "Sorceress")

    fun run(mode: HedgeMazeMode, print: (String) -> Unit): Boolean = runBlocking {
        runInternal(mode, print)
    }

    suspend fun runInternal(mode: HedgeMazeMode, print: (String) -> Unit): Boolean {
        val currentRoom = preferences.getInt("currentHedgeMazeRoom", 0)
        val turns = estimateMazeTurns(preferences, currentRoom)
        print(
            "You are currently in room $currentRoom and it will take you $turns turns to clear the maze.",
        )

        val adventuresLeft = character.state.value.adventuresLeft
        val lacking = turns - adventuresLeft
        if (lacking > 0) {
            print(
                "You need $lacking more adventure${if (lacking > 1) "s" else ""} " +
                    "to take that path through the maze.",
            )
            return false
        }

        if (adventuresLeft < 9) {
            preferences.setInt("choiceAdventure1004", 1)
        }

        val state = character.state.value
        val inDarkGyffte = state.ascensionPath == AscensionPath.DARK_GYFFTE
        if (mode == HedgeMazeMode.TRAPS) {
            val modifiers = buildCurrentModifiers()
            if (!wouldSurviveTraps(state, preferences, modifiers, inDarkGyffte)) {
                print("You won't survive to the end of the Hedge Maze.")
                return false
            }
        }

        if (mode != HedgeMazeMode.NUGGLETS && !inDarkGyffte) {
            healToMax()
        }

        print("Entering the Hedge Maze...")
        while (questDatabase.getProgress(Quest.FINAL) == "step4") {
            val path = "place.php?whichplace=nstower&action=ns_03_hedgemaze"
            val url = "$KOL_BASE_URL/$path"
            val html = try {
                val response = httpClient.get(url)
                if (!response.status.isSuccess()) return false
                response.bodyAsText()
            } catch (_: Exception) {
                return false
            }
            processQuestHooks(html, url)

            if (mode == HedgeMazeMode.TRAPS && !checkTrapSurvival(html)) {
                return false
            }
            if (html.contains("You don't have time")) {
                print("You're out of adventures.")
                return false
            }

            adventureManager.followAdventureResponse(mazeLocation, html, url)
        }

        val status = questDatabase.getProgress(Quest.FINAL)
        if (status == "step5") {
            print("Hedge Maze cleared!")
            return true
        }
        print("Hedge Maze not complete. Unexpected quest status: ${Quest.FINAL.prefKey} = $status")
        return false
    }

    private fun checkTrapSurvival(html: String): Boolean {
        val room = preferences.getInt("currentHedgeMazeRoom", 0)
        return when (room) {
            1 -> html.contains("lucky you survived that")
            4 -> html.contains("drag yourself out of the opposite end")
            7 -> html.contains("emerge from the tunnel")
            else -> true
        }
    }

    private suspend fun healToMax() {
        val recovery = recoveryManager ?: return
        val inv = inventoryManager ?: return
        val skills = skillManager ?: return
        repeat(20) {
            val state = character.state.value
            if (state.currentHp >= state.maxHp) return
            val recovered = recovery.recoverHpToMax(
                state,
                inv.state.value,
                skills.state.value,
                state.maxHp,
            )
            if (!recovered) return
        }
    }

    private fun buildCurrentModifiers(): CurrentModifiers {
        val state = character.state.value
        val effects = effectManager?.state?.value?.effects ?: emptyList()
        val passiveSkills = skillManager?.state?.value?.skills
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        return CurrentModifiers(state, effects, passiveSkills)
    }
}

internal fun GameRuntimeLibrary.runHedgeMaze(mode: HedgeMazeMode, print: (String) -> Unit): Boolean {
    val client = httpClient ?: return false
    val adventure = adventureManager ?: return false
    val char = character ?: return false
    val prefs = preferences ?: return false
    val db = questDatabase ?: return false
    return runBlocking {
        HedgeMazeRunner(
            httpClient = client,
            adventureManager = adventure,
            character = char,
            preferences = prefs,
            questDatabase = db,
            recoveryManager = recoveryManager,
            inventoryManager = inventoryManager,
            skillManager = skillManager,
            effectManager = effectManager,
            processQuestHooks = { html, url -> processVisitQuestHooks(html, url) },
        ).runInternal(mode, print)
    }
}
