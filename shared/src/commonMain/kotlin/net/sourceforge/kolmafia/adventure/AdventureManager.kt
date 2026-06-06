package net.sourceforge.kolmafia.adventure

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.mood.ManaBurnManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class AdventureManager(
    private val adventureRequest: AdventureRequest,
    private val fightRequest: FightRequest,
    private val choiceRequest: ChoiceRequest,
    private val characterRequest: CharacterRequest,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val eventBus: GameEventBus,
    private val registry: ChoiceHandlerRegistry = ChoiceHandlerRegistry(),
    internal val goalManager: GoalManager = GoalManager(),
    private val questDatabase: QuestDatabase = QuestDatabase(preferences),
    private val solvers: ChoiceSolvers = ChoiceSolvers.NoOp,
    private val inventory: InventoryManager? = null,
    private val effects: EffectManager? = null,
    private val skills: SkillManager? = null,
    private val recoveryManager: RecoveryManager? = null,
    private val moodManager: MoodManager? = null,
    private val questLogRequest: QuestLogRequest? = null,
    private val manaBurnManager: ManaBurnManager? = null,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var currentJob: Job? = null

    private var skillUses: Int = 0
    private var lastTurnResponseText: String = ""
    private var itemGoalMetThisTurn = false

    fun setSkillUses(n: Int) { skillUses = n }

    companion object {
        private val QUEST_ADVANCE_SIGNALS = listOf(
            "Quest Completed", "Quest Updated",
            "added to your Quest Log", "Your quest log has been updated",
        )
    }

    fun runAdventures(location: AdventureLocation, turns: Int, scope: CoroutineScope): Job =
        scope.launch {
            _isRunning.value = true
            try {
                repeat(turns) {
                    if (!isActive) return@launch
                    itemGoalMetThisTurn = false
                    // Re-buff before this adventure turn
                    moodManager?.executeActiveMood(
                        effectState = effects?.state?.value ?: EffectState(),
                        skillState  = skills?.state?.value ?: SkillState(),
                        charState   = character.state.value,
                    )
                    val result = doOneTurn(location) ?: return@launch

                    if (itemGoalMetThisTurn) {
                        eventBus.emit(GameEvent.TurnConsumed(location, result))
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("item goal met")))
                        return@launch
                    }

                    characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }

                    // Numeric goal checks (meat, level) — evaluated on up-to-date character state
                    val charAfterTurn = character.state.value
                    if (goalManager.hasMeatGoal(charAfterTurn.meat)) {
                        eventBus.emit(GameEvent.TurnConsumed(location, result))
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("meat goal met: ${charAfterTurn.meat}")))
                        return@launch
                    }
                    if (goalManager.hasLevelGoal(charAfterTurn.level)) {
                        eventBus.emit(GameEvent.TurnConsumed(location, result))
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("level goal met: ${charAfterTurn.level}")))
                        return@launch
                    }

                    // Recovery loop: repeat until stop threshold met or no recovery available (max 10 iterations)
                    val rm = recoveryManager
                    if (rm != null) {
                        var iter = 0
                        while (iter < 10) {
                            val force = iter > 0  // after first recovery, bypass trigger-threshold check
                            val healed = rm.recoverIfNeeded(
                                charState  = character.state.value,
                                invState   = inventory?.state?.value ?: InventoryState(),
                                skillState = skills?.state?.value ?: SkillState(),
                                force      = force,
                            )
                            iter++
                            if (!healed) break
                            characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
                            val s = character.state.value
                            val hpDone = !preferences.getBoolean(Preferences.AUTO_RECOVER_HP, true) ||
                                         RecoveryManager.hpAboveStopThreshold(s, preferences)
                            val mpDone = !preferences.getBoolean(Preferences.AUTO_RECOVER_MP, false) ||
                                         RecoveryManager.mpAboveStopThreshold(s, preferences)
                            if (hpDone && mpDone) break
                        }
                    }
                    // ManaBurn: cast lowest-duration effect skill while MP is above burn threshold
                    val mbm = manaBurnManager
                    if (mbm != null) {
                        var burnIter = 0
                        while (burnIter < 10) {
                            val burned = mbm.burnIfEnabled(
                                mood        = moodManager?.activeMood,
                                effectState = effects?.state?.value ?: EffectState(),
                                skillState  = skills?.state?.value ?: SkillState(),
                                charState   = character.state.value,
                            )
                            burnIter++
                            if (!burned) break
                            characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
                        }
                    }
                    checkQuestAdvancement(lastTurnResponseText)
                    eventBus.emit(GameEvent.TurnConsumed(location, result))

                    when {
                        character.state.value.adventuresLeft <= 0 -> {
                            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NoAdventuresLeft))
                            return@launch
                        }
                        character.state.value.currentHp <= 0 -> {
                            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.CharacterDeath))
                            return@launch
                        }
                    }
                }
            } catch (e: CancellationException) {
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.UserCancelled))
                throw e
            } catch (e: Exception) {
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(e)))
            } finally {
                _isRunning.value = false
            }
        }.also { currentJob = it }

    fun stop() { currentJob?.cancel() }

    internal suspend fun checkQuestAdvancement(responseText: String) {
        if (QUEST_ADVANCE_SIGNALS.none { responseText.contains(it, ignoreCase = true) }) return
        questLogRequest?.syncAll()
    }

    private suspend fun doOneTurn(location: AdventureLocation): AdventureResult? {
        val (html, url) = adventureRequest.adventure(location).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        lastTurnResponseText = html
        return when (val parsed = AdventureParser.parseAdventureResponse(html, url)) {
            is AdventureResult.Combat -> resolveCombat(location)
            is AdventureResult.Choice -> resolveChoice(parsed.choiceId, parsed.responseText)
            is AdventureResult.NonCombat -> parsed.also { emitItemEvents(it.itemsGained) }
        }
    }

    private suspend fun resolveCombat(location: AdventureLocation): AdventureResult.Combat? {
        val macro = MacroStrategy.forLocation(location.id, preferences)
        val fightHtml = fightRequest.fight(macro).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        val result = AdventureParser.parseFightResult(fightHtml)
        eventBus.emit(GameEvent.CombatFinished(result.won, result.monster))
        emitItemEvents(result.itemsGained)
        if (!result.won) {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.CharacterDeath))
            return null
        }
        return result
    }

    private suspend fun resolveChoice(
        choiceId: Int,
        responseText: String,
    ): AdventureResult.Choice {
        val ctx = ChoiceContext(
            choiceId       = choiceId,
            options        = ChoiceUtilities.parseChoices(responseText),
            responseText   = responseText,
            characterState = character.state.value,
            inventoryState = inventory?.state?.value ?: InventoryState(),
            effectState    = effects?.state?.value ?: EffectState(),
            skillState     = skills?.state?.value ?: SkillState(),
            preferences    = preferences,
            goalManager    = goalManager,
            questDatabase  = questDatabase,
            solvers        = solvers,
            preference     = preferences.getInt("choiceAdventure$choiceId", 0),
            // TODO: track step count across the adventure loop and pass it here
            stepCount      = 0,
            skillUses      = skillUses,
        )
        val option = registry.dispatch(ctx) ?: preferences.getString("choiceAdventure$choiceId").toIntOrNull() ?: 1
        if (option > 0 && skillUses > 0) {
            skillUses--
        }
        choiceRequest.choose(choiceId, option)
        val resolved = AdventureResult.Choice(choiceId, "Choice Adventure", chosenOption = option)
        eventBus.emit(GameEvent.ChoiceResolved(choiceId, option))
        return resolved
    }

    private suspend fun emitItemEvents(items: List<String>) {
        items.forEach { name ->
            eventBus.emit(GameEvent.ItemObtained(InventoryItem(-1, name, 1, ItemType.OTHER)))
            if (goalManager.hasItemGoalByName(name)) itemGoalMetThisTurn = true
        }
    }
}
