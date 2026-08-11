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
import net.sourceforge.kolmafia.combat.EncounterModifierPipeline
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.combat.RandomModifierParser
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.FightPokefamSync
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ZoneLookup
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.SessionMeatSync
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.quest.FinalQuestCombatSync
import net.sourceforge.kolmafia.quest.GuzzlrCombatSync
import net.sourceforge.kolmafia.quest.IslandWarCombatSync
import net.sourceforge.kolmafia.quest.ToppingPeakCombatSync
import net.sourceforge.kolmafia.quest.MonsterConsequenceSync
import net.sourceforge.kolmafia.quest.ShadowRiftSync
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
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.request.BarrelChoiceMapper
import net.sourceforge.kolmafia.session.BarrelShrineSync
import net.sourceforge.kolmafia.session.BastilleBattalionSync
import net.sourceforge.kolmafia.session.BastilleSyncContext
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.session.SkillLearnFromResponse
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.IntergnatDemonNameSync
import net.sourceforge.kolmafia.request.AlliedRadioRequest
import net.sourceforge.kolmafia.request.OceanRequest
import net.sourceforge.kolmafia.session.DemonInCombatNameSync
import net.sourceforge.kolmafia.session.YegDemonNameSync
import net.sourceforge.kolmafia.session.CargoPocketSync
import net.sourceforge.kolmafia.character.FamTeamSync
import net.sourceforge.kolmafia.session.OceanManager
import net.sourceforge.kolmafia.session.WereProfessorResearchSync
import net.sourceforge.kolmafia.session.WildfireCampManager
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
    private val adventureSpentTracker: AdventureSpentTracker? = null,
    private val dreadKissesTracker: DreadKissesTracker? = null,
    private val intergnatDemonNameSync: IntergnatDemonNameSync? = null,
    private val yegDemonNameSync: YegDemonNameSync? = null,
    private val cargoPocketSync: CargoPocketSync? = null,
    private val demonInCombatNameSync: DemonInCombatNameSync? = null,
    private val sessionLogger: SessionLogger? = null,
    private val oceanRequest: OceanRequest? = null,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var currentJob: Job? = null

    private var skillUses: Int = 0
    private var lastTurnResponseText: String = ""
    private var lastTurnUrl: String = ""
    private var itemGoalMetThisTurn = false
    private var _inMultiFight = false
    private var _fightFollowsChoice = false
    private var _inChoiceResolution = false
    private var lastFightHtml: String = ""

    val inMultiFight: Boolean get() = _inMultiFight
    val fightFollowsChoice: Boolean get() = _fightFollowsChoice
    val inChoiceResolution: Boolean get() = _inChoiceResolution

    /** Desktop ChoiceManager.canWalkAway — stub true until choice HTML walk-away parity. */
    fun canWalkAwayFromChoice(): Boolean = true

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

    internal fun testSetChoiceResolution(inChoiceResolution: Boolean) {
        _inChoiceResolution = inChoiceResolution
    }

    fun setSkillUses(n: Int) { skillUses = n }

    private suspend fun emitTurnConsumed(location: AdventureLocation, result: AdventureResult) {
        adventureSpentTracker?.recordNoncombatIfNeeded(location, result)
        adventureSpentTracker?.addTurn(location.name)
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
                    QuantumTerrariumRequest.checkCounter(
                        client = characterRequest.client,
                        character = character,
                        preferences = preferences,
                        url = lastTurnUrl,
                        hasResult = lastTurnResponseText.isNotBlank(),
                        sessionLogger = sessionLogger,
                    )
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
        lastTurnUrl = url
        return when (val parsed = AdventureParser.parseAdventureResponse(html, url)) {
            is AdventureResult.Combat -> resolveCombat(location)
            is AdventureResult.Choice -> {
                preferences.setInt(LAST_CHOICE_ID, parsed.choiceId)
                val choiceResult = resolveChoice(parsed.choiceId, parsed.responseText)
                if (_fightFollowsChoice && _inMultiFight) resolveCombat(location) ?: choiceResult
                else choiceResult
            }
            is AdventureResult.NonCombat -> parsed.also {
                SessionMeatSync.apply(character, it.text)
                emitItemEvents(it.itemsGained)
            }
        }
    }

    private suspend fun resolveCombat(location: AdventureLocation): AdventureResult.Combat? {
        if (!_inMultiFight) {
            DefaultsDatabase.resetOnFightPrefs(preferences)
        }
        if (lastTurnResponseText.isNotBlank()) {
            lastFightHtml = lastTurnResponseText
        }
        prepareCombatMonster(lastTurnResponseText)
        val macro = combatMacroResolver?.invoke(location.id)
            ?: MacroStrategy.forLocation(location.id, preferences)
        val fightHtml = fightRequest.fight(macro).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        lastFightHtml = fightHtml
        FightPokefamSync.apply(character, fightHtml, familiarManager, preferences, sessionLogger)
        _inMultiFight = AdventureParser.isInMultiFight(fightHtml)
        val result = AdventureParser.parseFightResult(fightHtml)
        SessionMeatSync.apply(character, fightHtml)
        SkillLearnFromResponse.learnSkillFromResponse(
            fightHtml,
            preferences,
            skills,
            inventory,
        )
        if (!_inMultiFight) _fightFollowsChoice = false
        dreadKissesTracker?.updateFromFight(location.name, fightHtml)
        intergnatDemonNameSync?.updateFromFight(
            fightHtml,
            familiarId = character.state.value.familiarId,
            randomModifiers = MonsterStatusTracker.getLastMonster()?.randomModifiers.orEmpty(),
        )
        eventBus.emit(GameEvent.CombatFinished(result.won, result.monster))
        if (result.won) {
            edServantManager?.addCombatExperience()
        }
        if (result.monster.isNotEmpty() && MonsterStatusTracker.getLastMonster() == null) {
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
            val combatResult = QuestFightRules.applyCombat(
                it, result.monster, result.won, result.itemsGained, itemIdsGained,
                preferences, location.id,
                responseText = fightHtml,
                hasItemEquipped = { id ->
                    character.state.value.equipment.values.any { name ->
                        gameDatabase?.item(name)?.id == id
                    }
                },
            )
            if (combatResult.resyncQuestLogPage1) {
                questLogRequest?.syncPage(1)
                val woots = preferences.getString("_questPartyFairProgress", "0")
                sessionLogger?.appendRawLine("The Party is at $woots/100 woots.")
            }
            QuestItemRules.applyItemsGained(
                result.itemsGained,
                it,
                hasItemId = { id -> inventory?.state?.value?.items?.containsKey(id) == true },
                consumeItem = { itemId, quantity ->
                    inventory?.consumeItemLocally(itemId, quantity)
                },
            )
            GuzzlrCombatSync.applyCombatWin(
                questDatabase = it,
                preferences = preferences,
                locationName = location.name,
                responseText = fightHtml,
                won = result.won,
                gameDatabase = gameDatabase,
                hasItemEquipped = { id ->
                    character.state.value.equipment.values.any { name ->
                        gameDatabase?.item(name)?.id == id
                    }
                },
                hasItemCount = { id -> inventory?.state?.value?.items?.get(id)?.quantity ?: 0 },
                consumeItem = { itemId, quantity ->
                    inventory?.consumeItemLocally(itemId, quantity)
                },
            )
            FinalQuestCombatSync.applyCombatWin(
                questDatabase = it,
                preferences = preferences,
                adventureId = location.id,
                monster = result.monster,
                responseText = fightHtml,
                won = result.won,
            )
            ToppingPeakCombatSync.applyCombatWin(
                preferences = preferences,
                monster = result.monster,
                responseText = fightHtml,
                won = result.won,
                hasItemEquipped = { id ->
                    character.state.value.equipment.values.any { name ->
                        gameDatabase?.item(name)?.id == id
                    }
                },
            )
            val warEnded = IslandWarCombatSync.applyEndOfWar(
                questDatabase = it,
                preferences = preferences,
                adventureId = location.id,
                monster = result.monster,
                responseText = fightHtml,
                won = result.won,
                isKingdomOfExploathing = character.state.value.isKingdomOfExploathing,
            )
            if (!warEnded) {
                IslandWarCombatSync.applyCombatWin(
                    preferences = preferences,
                    adventureId = location.id,
                    responseText = fightHtml,
                    won = result.won,
                    isKingdomOfExploathing = character.state.value.isKingdomOfExploathing,
                )
            }
            IslandWarCombatSync.applyNunsSidequestWin(
                preferences = preferences,
                monster = result.monster,
                responseText = fightHtml,
                won = result.won,
            )
        }
        emitItemEvents(result.itemsGained)
        if (preferences.getString(Preferences.LAST_LOCATION, "").let { ShadowRiftSync.isShadowRiftLocation(it) }) {
            RufusManager(preferences).handleShadowRiftFight(result.monster)
            ShadowRiftSync.incrementCombats(preferences)
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

    private fun prepareCombatMonster(adventureHtml: String) {
        if (adventureHtml.isBlank() || gameDatabase == null) return
        val displayName = AdventureParser.parseEncounterMonsterName(adventureHtml) ?: return
        val parsed = RandomModifierParser.parseRandomModifiers(displayName, adventureHtml)
        val modifiers = parsed.modifiers.toMutableList()
        val charState = character.state.value
        val strippedName = EncounterModifierPipeline.applyPostOcrs(
            parsed.strippedName,
            modifiers,
            EncounterModifierPipeline.EncounterModifierContext(
                familiarId = charState.familiarId,
                ascensionPath = charState.ascensionPath,
            ),
        )
        val disambiguatedName = MonsterConsequenceSync.disambiguateMonster(strippedName, adventureHtml)
        val template = RandomModifierParser.resolveTemplate(
            disambiguatedName,
            adventureHtml,
            gameDatabase,
        ) ?: gameDatabase.monster(disambiguatedName)
            ?: gameDatabase.monster(displayName)
            ?: return
        MonsterStatusTracker.setNextMonster(template, modifiers)
        preferences.setString(Preferences.LAST_MONSTER, template.name)
    }

    internal suspend fun resolveChoice(
        choiceId: Int,
        initialResponseText: String,
    ): AdventureResult.Choice {
        _inChoiceResolution = true
        try {
            return resolveChoiceLoop(choiceId, initialResponseText)
        } finally {
            _inChoiceResolution = false
        }
    }

    private suspend fun resolveChoiceLoop(
        choiceId: Int,
        initialResponseText: String,
    ): AdventureResult.Choice {
        var currentChoiceId     = choiceId
        var currentResponseText = initialResponseText
        var stepCount           = 0
        var lastChosenOption    = 1
        val maxSteps            = 20

        while (stepCount < maxSteps) {
            if (WereProfessorResearchSync.isResearchBenchChoice(currentChoiceId)) {
                WereProfessorResearchSync.visitChoice(currentResponseText, preferences)
            }
            if (currentChoiceId == BarrelChoiceMapper.CHOICE_ID) {
                BarrelShrineSync.syncFromVisit(currentResponseText, preferences)
            }
            if (BastilleBattalionSync.isBastilleChoice(currentChoiceId)) {
                val bastilleContext = bastilleSyncContext()
                BastilleBattalionSync.syncVisit(
                    currentChoiceId, currentResponseText, url = null, preferences, bastilleContext,
                )
            }
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
                gameDatabase   = gameDatabase,
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

            if (BastilleBattalionSync.isBastilleChoice(currentChoiceId)) {
                val bastilleContext = bastilleSyncContext()
                BastilleBattalionSync.registerRequest(currentChoiceId, option, preferences, bastilleContext)
                BastilleBattalionSync.syncPreChoice(currentChoiceId, option, preferences, bastilleContext)
            }
            val extraFormFields = cargoPocketFormFields(currentChoiceId, option, ctx)
            val (rawHtml, rawUrl) = choiceRequest.choose(currentChoiceId, option, extraFormFields).getOrElse { e ->
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(e)))
                return AdventureResult.Choice(currentChoiceId, "Choice Adventure", chosenOption = option)
            }
            var html = rawHtml
            var url = rawUrl
            WereProfessorResearchSync.postChoice0(url, html, sessionLogger)
            OceanManager.registerRequest(url, sessionLogger)
            FamTeamSync.registerRequest(url, sessionLogger)
            if (OceanRequest.isOceanPage(html, url) && OceanManager.shouldAutomate(preferences)) {
                oceanRequest?.let { request ->
                    when (
                        val oceanResult = OceanManager.processOceanAdventure(
                            request,
                            preferences,
                            sessionLogger,
                        )
                    ) {
                        is OceanManager.OceanResult.Stop -> {
                            eventBus.emit(
                                GameEvent.AdventureLoopStopped(StopReason.GoalMet(oceanResult.message)),
                            )
                            return AdventureResult.Choice(
                                currentChoiceId,
                                "Choice Adventure",
                                chosenOption = option,
                            )
                        }
                        is OceanManager.OceanResult.Continued -> {
                            html = oceanResult.html
                            url = oceanResult.url
                        }
                        is OceanManager.OceanResult.Manual -> Unit
                    }
                }
            }
            syncCargoPocketPick(currentChoiceId, option, extraFormFields, html)
            syncCargoPocketVisit(currentChoiceId, html)
            syncAlliedRadioResponse(currentChoiceId, html)
            SessionMeatSync.apply(character, html)
            if (BastilleBattalionSync.isBastilleChoice(currentChoiceId)) {
                val effectNames = effects?.state?.value?.effects?.map { it.name }?.toSet() ?: emptySet()
                BastilleBattalionSync.syncPostChoice(
                    currentChoiceId, option, html, preferences, effectNames, bastilleSyncContext(),
                )
            }
            if (currentChoiceId == BarrelChoiceMapper.CHOICE_ID) {
                BarrelShrineSync.syncPostChoice(option, preferences)
            }
            if (WereProfessorResearchSync.isResearchBenchChoice(currentChoiceId)) {
                WereProfessorResearchSync.registerRequest(url, sessionLogger)
                WereProfessorResearchSync.postChoice2(url, html, preferences, sessionLogger)
                if (AdventureParser.parseAdventureResponse(html, url) is AdventureResult.Choice) {
                    WereProfessorResearchSync.visitChoice(html, preferences)
                }
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

            val next = AdventureParser.parseAdventureResponse(html, url)
            if (next is AdventureResult.Combat) {
                syncCargoPocketFight(currentChoiceId, option, extraFormFields)
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
        lastTurnUrl = url
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

    private fun cargoPocketFormFields(
        choiceId: Int,
        option: Int,
        ctx: ChoiceContext,
    ): Map<String, String> {
        if (choiceId != CargoPocketSync.CARGO_CULT_CHOICE || option != 1) return emptyMap()
        val pocket = ctx.preferences.getString("choiceAdventure$choiceId").toIntOrNull()
            ?: return emptyMap()
        return mapOf("pocket" to pocket.toString())
    }

    private fun syncCargoPocketPick(
        choiceId: Int,
        option: Int,
        extraFormFields: Map<String, String>,
        html: String,
    ) {
        if (choiceId != CargoPocketSync.CARGO_CULT_CHOICE || option != 1) return
        val sync = cargoPocketSync ?: return
        val pocket = extraFormFields["pocket"]?.toIntOrNull()
            ?: preferences.getString("choiceAdventure$choiceId").toIntOrNull()
            ?: return
        sync.parsePocketPick(pocket, html)
    }

    private fun syncCargoPocketFight(
        choiceId: Int,
        option: Int,
        extraFormFields: Map<String, String>,
    ) {
        if (choiceId != CargoPocketSync.CARGO_CULT_CHOICE || option != 1) return
        val sync = cargoPocketSync ?: return
        val pocket = extraFormFields["pocket"]?.toIntOrNull()
            ?: preferences.getString("choiceAdventure$choiceId").toIntOrNull()
            ?: return
        sync.registerPocketFightFromPocket(pocket)
    }

    private fun syncCargoPocketVisit(choiceId: Int, html: String) {
        if (choiceId != CargoPocketSync.CARGO_CULT_CHOICE) return
        cargoPocketSync?.parseAvailablePockets(html)
    }

    private fun syncAlliedRadioResponse(choiceId: Int, html: String) {
        if (!DemonInCombatNameSync.isAlliedRadioChoice(choiceId)) return
        preferences?.let { AlliedRadioRequest.parseVisitChoice(html, it) }
        demonInCombatNameSync?.parseRadioResponse(html)
    }

    private fun bastilleSyncContext(): BastilleSyncContext =
        BastilleSyncContext(
            sessionLogger = sessionLogger,
            playerId = character.state.value.playerId,
        )
}
