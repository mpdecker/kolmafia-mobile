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
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ZoneLookup
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PirateRealmSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.QuestItemRules
import net.sourceforge.kolmafia.quest.QuestLogSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.mood.ManaBurnManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.ash.ScriptHookRunner
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
    private val banishManager: BanishManager? = null,
    private val combatDatabase: ZoneLookup? = null,
    private val gameDatabase: GameDatabase? = null,
    private val outfitManager: OutfitManager? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val useItemRequest: UseItemRequest? = null,
    private val familiarManager: FamiliarManager? = null,
    private val scriptHookRunner: ScriptHookRunner? = null,
    private val combatMacroResolver: ((String) -> String)? = null,
    private val edServantManager: net.sourceforge.kolmafia.servant.EdServantManager? = null,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var currentJob: Job? = null

    private var skillUses: Int = 0
    private var lastTurnResponseText: String = ""
    private var itemGoalMetThisTurn = false
    private var _inMultiFight = false
    private var _fightFollowsChoice = false
    private var lastFightHtml: String = ""

    val inMultiFight: Boolean get() = _inMultiFight
    val fightFollowsChoice: Boolean get() = _fightFollowsChoice

    fun canStillSteal(): Boolean {
        if (!net.sourceforge.kolmafia.character.CharacterStats.canPickpocket(
                character.state.value,
                ::hasPickpocketSkill,
            )) {
            return false
        }
        return AdventureParser.canStillSteal(lastFightHtml)
    }

    private fun hasPickpocketSkill(name: String): Boolean =
        skills?.state?.value?.skills?.any { it.name.equals(name, ignoreCase = true) } == true

    internal fun testSetLastFightHtml(html: String) {
        lastFightHtml = html
    }

    internal fun testSetCombatFlags(inMultiFight: Boolean, fightFollowsChoice: Boolean) {
        _inMultiFight = inMultiFight
        _fightFollowsChoice = fightFollowsChoice
    }

    fun setSkillUses(n: Int) { skillUses = n }

    private suspend fun emitTurnConsumed(location: AdventureLocation, result: AdventureResult) {
        eventBus.emit(GameEvent.TurnConsumed(location, result))
        scriptHookRunner?.onTurnConsumed()
    }

    fun runAdventures(location: AdventureLocation, turns: Int, scope: CoroutineScope): Job =
        scope.launch {
            _isRunning.value = true
            try {
                if (!AdventurePrep.canAdventureAtZone(
                        location.name,
                        character.state.value,
                        preferences = preferences,
                    )) {
                    eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.MacroError("cannot adventure at ${location.name}")))
                    return@launch
                }
                if (!AdventurePrep.prepareForAdventure(
                        location.name,
                        outfitManager,
                        preferences,
                        retrieveItemService,
                        useItemRequest,
                        gameDatabase,
                        familiarManager,
                        character.state.value,
                    )) {
                    eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.MacroError("prepare for adventure failed")))
                    return@launch
                }

                repeat(turns) {
                    if (!isActive) return@launch
                    itemGoalMetThisTurn = false

                    // Zone pre-flight: if all monsters in the zone are banished, stop immediately
                    val bm = banishManager
                    val zoneData = combatDatabase?.getByLocation(location.name)
                    if (bm != null && zoneData != null) {
                        val currentTurn = character.state.value.currentRun
                        val positiveWeightMonsters = zoneData.monsters.filter { it.weight > 0 }
                        if (positiveWeightMonsters.isNotEmpty() &&
                            positiveWeightMonsters.all { bm.isBanished(it.name, currentTurn) }) {
                            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.AllMonstersBanished))
                            return@launch
                        }
                    }

                    // Re-buff before this adventure turn
                    moodManager?.executeActiveMood(
                        effectState = effects?.state?.value ?: EffectState(),
                        skillState  = skills?.state?.value ?: SkillState(),
                        charState   = character.state.value,
                    )
                    val result = doOneTurn(location) ?: return@launch

                    if (itemGoalMetThisTurn) {
                        emitTurnConsumed(location, result)
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("item goal met")))
                        return@launch
                    }

                    checkInventoryItemGoals()

                    characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }

                    // Numeric goal checks (meat, level) — evaluated on up-to-date character state
                    val charAfterTurn = character.state.value
                    if (goalManager.hasMeatGoal(charAfterTurn.meat)) {
                        emitTurnConsumed(location, result)
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("meat goal met: ${charAfterTurn.meat}")))
                        return@launch
                    }
                    if (goalManager.hasLevelGoal(charAfterTurn.level)) {
                        emitTurnConsumed(location, result)
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("level goal met: ${charAfterTurn.level}")))
                        return@launch
                    }
                    if (goalManager.matchesFactoid(lastTurnResponseText)) {
                        emitTurnConsumed(location, result)
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("factoid goal met")))
                        return@launch
                    }
                    if (goalManager.matchesSubstats(lastTurnResponseText)) {
                        goalManager.clearSubstatsGoal()
                        emitTurnConsumed(location, result)
                        eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("substats goal met")))
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
                    // ManaBurn: cast lowest-duration effect skill while MP is above burn threshold.
                    // skillState is re-read from SkillManager.state each iteration; correctness
                    // requires that SkillManager.cast() updates timesCast in that StateFlow
                    // synchronously before returning, so daily-limit checks stay accurate.
                    val mbm = manaBurnManager
                    if (mbm != null) {
                        var burnIter = 0
                        while (burnIter < 10) {
                            val burned = mbm.burnIfEnabled(
                                mood        = moodManager?.activeMood,
                                effectState = effects?.state?.value ?: EffectState(),
                                skillState  = skills?.state?.value ?: SkillState(),
                                charState   = character.state.value,
                                moodLibrary = moodManager?.moodLibrary ?: emptyMap(),
                            )
                            burnIter++
                            if (!burned) break
                            characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
                        }
                    }
                    checkQuestAdvancement(lastTurnResponseText)
                    TurnCounter.removeExpired(preferences, character.state.value.currentRun)
                    emitTurnConsumed(location, result)

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
        questDatabase?.let { db ->
            val itemCount: (Int) -> Int = { id ->
                inventory?.state?.value?.items?.get(id)?.quantity ?: 0
            }
            QuestItemRules.applyInventory(itemCount, db)
        }
        QuestLogSync.processResponse(
            responseText,
            questDatabase,
            questLogRequest,
            buildQuestSyncContext(),
        )
    }

    private fun buildQuestSyncContext(): QuestLogSync.QuestSyncContext =
        QuestLogSync.QuestSyncContext(
            hasItemId = { id -> inventory?.state?.value?.items?.containsKey(id) == true },
            preferences = preferences,
            currentRun = character.state.value.currentRun,
            gameDatabase = gameDatabase,
        )

    private suspend fun doOneTurn(location: AdventureLocation): AdventureResult? {
        val (html, url) = adventureRequest.adventure(location).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        lastTurnResponseText = html
        return when (val parsed = AdventureParser.parseAdventureResponse(html, url)) {
            is AdventureResult.Combat -> resolveCombat(location)
            is AdventureResult.Choice -> {
                preferences.setInt(LAST_CHOICE_ID, parsed.choiceId)
                val choiceResult = resolveChoice(parsed.choiceId, parsed.responseText)
                if (_fightFollowsChoice && _inMultiFight) resolveCombat(location) ?: choiceResult
                else choiceResult
            }
            is AdventureResult.NonCombat -> parsed.also { emitItemEvents(it.itemsGained) }
        }
    }

    private suspend fun resolveCombat(location: AdventureLocation): AdventureResult.Combat? {
        if (lastTurnResponseText.isNotBlank()) {
            lastFightHtml = lastTurnResponseText
        }
        val macro = combatMacroResolver?.invoke(location.id)
            ?: MacroStrategy.forLocation(location.id, preferences)
        val fightHtml = fightRequest.fight(macro).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        lastFightHtml = fightHtml
        _inMultiFight = AdventureParser.isInMultiFight(fightHtml)
        val result = AdventureParser.parseFightResult(fightHtml)
        if (!_inMultiFight) _fightFollowsChoice = false
        eventBus.emit(GameEvent.CombatFinished(result.won, result.monster))
        if (result.won) {
            edServantManager?.addCombatExperience()
        }
        if (result.monster.isNotEmpty()) {
            preferences.setString(Preferences.LAST_MONSTER, result.monster)
        }
        questDatabase?.let {
            PirateRealmSync.applyWindicleFromFightHtml(fightHtml, location.id, it, preferences)
        }
        val gainedVolcanoMap = result.itemsGained.any { it.contains("volcano map", ignoreCase = true) } ||
            result.itemsGained.any { gameDatabase?.item(it)?.id == QuestFightRules.VOLCANO_MAP_ID }
        if (TurnCounter.NEMESIS_ASSASSIN_MONSTERS.any {
                result.monster.equals(it, ignoreCase = true)
            } || gainedVolcanoMap
        ) {
            TurnCounter.resetNemesisAssassinWindow(
                preferences,
                character.state.value.currentRun,
            )
        }
        questDatabase?.let {
            QuestFightRules.applyFightStarted(it, result.monster)
            val itemIdsGained = result.itemsGained.mapNotNull { name -> gameDatabase?.item(name)?.id }
            QuestFightRules.applyCombat(
                it, result.monster, result.won, result.itemsGained, itemIdsGained,
                preferences, location.id,
            )
            QuestItemRules.applyItemsGained(result.itemsGained, it)
        }
        emitItemEvents(result.itemsGained)
        if (preferences.getString(Preferences.LAST_LOCATION, "").contains("Shadow Rift", ignoreCase = true)) {
            RufusManager(preferences).handleShadowRiftFight(result.monster)
        }
        if (result.banished) {
            eventBus.emit(GameEvent.MonsterBanished(result.monster, result.banisher.canonicalName))
            banishManager?.banishMonster(
                monsterName = result.monster,
                banisher    = result.banisher,
                currentTurn = character.state.value.currentRun,
            )
            return result  // banish is a successful combat resolution -- do not treat as death
        }
        if (!result.won) {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.CharacterDeath))
            return null
        }
        return result
    }

    internal suspend fun resolveChoice(
        choiceId: Int,
        initialResponseText: String,
    ): AdventureResult.Choice {
        var currentChoiceId     = choiceId
        var currentResponseText = initialResponseText
        var stepCount           = 0
        var lastChosenOption    = 1
        val maxSteps            = 20

        while (stepCount < maxSteps) {
            val ctx = ChoiceContext(
                choiceId       = currentChoiceId,
                options        = ChoiceUtilities.parseChoices(currentResponseText),
                responseText   = currentResponseText,
                characterState = character.state.value,
                inventoryState = inventory?.state?.value ?: InventoryState(),
                effectState    = effects?.state?.value ?: EffectState(),
                skillState     = skills?.state?.value ?: SkillState(),
                preferences    = preferences,
                goalManager    = goalManager,
                questDatabase  = questDatabase,
                solvers        = solvers,
                preference     = preferences.getInt("choiceAdventure$currentChoiceId", 0),
                stepCount      = stepCount,
                skillUses      = skillUses,
            )
            val option = registry.dispatch(ctx)
                ?: preferences.getString("choiceAdventure$currentChoiceId").toIntOrNull()
                ?: 1
            val optionLabel = ctx.options[option]
            // skillUses decremented once per step — each choice interaction costs one skill use budget unit
            if (option > 0 && skillUses > 0) skillUses--
            lastChosenOption = option

            val html = choiceRequest.choose(currentChoiceId, option).getOrElse { e ->
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(e)))
                return AdventureResult.Choice(currentChoiceId, "Choice Adventure", chosenOption = option)
            }
            questDatabase?.let {
                QuestChoiceRules.apply(
                    currentChoiceId, html, it, option, preferences, inventory, optionLabel,
                )
            }
            eventBus.emit(GameEvent.ChoiceResolved(currentChoiceId, option))
            if (goalManager.hasChoiceGoal(currentChoiceId)) {
                goalManager.clearChoiceGoal()
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.GoalMet("choice goal met: $currentChoiceId")))
                return AdventureResult.Choice(currentChoiceId, "Choice Adventure", chosenOption = option)
            }

            val next = AdventureParser.parseAdventureResponse(html, "")
            if (next is AdventureResult.Combat) {
                _fightFollowsChoice = true
                _inMultiFight = true
                break
            }
            if (next is AdventureResult.Choice) {
                currentChoiceId     = next.choiceId
                currentResponseText = next.responseText
                stepCount++
            } else {
                break
            }
        }
        if (stepCount >= maxSteps) {
            eventBus.emit(GameEvent.AdventureLoopStopped(
                StopReason.MacroError("Choice chain exceeded $maxSteps steps at choice $currentChoiceId")
            ))
        }
        return AdventureResult.Choice(currentChoiceId, "Choice Adventure", chosenOption = lastChosenOption)
    }

    internal suspend fun followAdventureResponse(
        location: AdventureLocation,
        html: String,
        url: String,
    ): AdventureResult? {
        lastTurnResponseText = html
        return when (val parsed = AdventureParser.parseAdventureResponse(html, url)) {
            is AdventureResult.Combat -> resolveCombat(location)
            is AdventureResult.Choice -> {
                preferences.setInt(LAST_CHOICE_ID, parsed.choiceId)
                val choiceResult = resolveChoice(parsed.choiceId, parsed.responseText)
                if (_fightFollowsChoice && _inMultiFight) resolveCombat(location) ?: choiceResult
                else choiceResult
            }
            is AdventureResult.NonCombat -> parsed
        }
    }

    companion object {
        const val LAST_CHOICE_ID = "_lastChoiceId"
    }

    private suspend fun emitItemEvents(items: List<String>) {
        items.forEach { name ->
            eventBus.emit(GameEvent.ItemObtained(InventoryItem(-1, name, 1, ItemType.OTHER)))
            if (goalManager.hasItemGoalByName(name)) itemGoalMetThisTurn = true
            val itemId = gameDatabase?.item(name)?.id
            if (itemId != null && goalManager.hasItemGoal(itemId)) itemGoalMetThisTurn = true
        }
    }

    private fun checkInventoryItemGoals() {
        if (!goalManager.hasItemGoals()) return
        val items = inventory?.state?.value?.items ?: return
        for (goalId in goalManager.itemGoalIds()) {
            if ((items[goalId]?.quantity ?: 0) > 0) {
                itemGoalMetThisTurn = true
                return
            }
        }
    }
}
