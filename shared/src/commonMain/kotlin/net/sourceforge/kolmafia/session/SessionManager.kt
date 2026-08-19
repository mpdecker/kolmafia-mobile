package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.faxbot.FaxBotDatabase
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.clan.ClanHotdogMenuCache
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionAvailableIngredients
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredientSources
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.TCRSDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandWarResetSync
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.FamTeamRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.LoginResult
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.shop.ShopRowDatabase
import net.sourceforge.kolmafia.skill.SkillManager

sealed class SessionState {
    object LoggedOut : SessionState()
    object LoggedIn : SessionState()
    data class Error(val message: String) : SessionState()
}

open class SessionManager(
    private val loginRequest: LoginRequest,
    private val characterRequest: CharacterRequest,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val inventoryManager: InventoryManager,
    private val familiarManager: FamiliarManager,
    private val skillManager: SkillManager,
    private val effectManager: EffectManager,
    private val scriptManager: ScriptManager,
    private val gameDatabase: GameDatabase,
    private val dailyResourceTracker: DailyResourceTracker,
    private val questLogRequest: QuestLogRequest? = null,
    private val moodManager: MoodManager? = null,
    private val banishManager: BanishManager? = null,
    private val breakfastManager: BreakfastManager? = null,
    private val outfitManager: OutfitManager? = null,
    private val sessionLogger: SessionLogger? = null,
    private val gameRuntimeLibrary: GameRuntimeLibrary? = null,
    private val junkListManager: JunkListManager? = null,
    private val httpClient: HttpClient? = null,
    private val closetRequest: ClosetRequest? = null,
    private val storageRequest: StorageRequest? = null,
    private val clanStashRequest: ClanStashRequest? = null,
    private val displayCaseRequest: DisplayCaseRequest? = null,
) {
    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun login(username: String, password: String): SessionState {
        return when (val loginResult = loginRequest.login(username, password)) {
            is LoginResult.Success -> {
                ClanManager.clearCache(newCharacter = true)
                preferences.setString(Preferences.LAST_USERNAME, username)
                gameDatabase.load()
                DefaultsDatabase.seedMissingDefaults(preferences)
                ShopRowDatabase.restoreLearnedRows(preferences)
                characterRequest.fetchCharacterState().fold(
                    onSuccess = { apiResponse ->
                        character.updateFromApiResponse(apiResponse)
                        val charState = character.state.value
                        val ascended = DefaultsDatabase.applyAscensionResetIfNeeded(
                            preferences,
                            charState.ascensionNumber,
                        )
                        IslandWarResetSync.ensureUpdated(charState.ascensionNumber, preferences)
                        VoteMonsterManager.checkCounter(preferences, charState.turnsPlayed)
                        dailyResourceTracker.syncDay(charState.dayCount)

                        // Gate rollover clear on day change or rollover timestamp gap (desktop parity)
                        val lastDay = preferences.getInt(Preferences.LAST_DAYCOUNT, -1)
                        val dayChanged = charState.dayCount != lastDay
                        val rolloverGap = RolloverCounterReset.shouldResetCounters(
                            charState.rolloverTimestamp,
                            preferences.getLong(RolloverCounterReset.LAST_COUNTER_DAY, -1L),
                        )
                        val rolloverChanged = dayChanged || rolloverGap
                        if (rolloverChanged) {
                            RolloverCounterReset.resetCounters(
                                preferences,
                                charState.rolloverTimestamp,
                                charState.currentRun,
                                banishManager,
                            )
                            banishManager?.clearExpiredAndRollover(charState.currentRun)
                            breakfastManager?.clearBreakfastPrefs()
                            preferences.setInt(Preferences.LAST_DAYCOUNT, charState.dayCount)
                        }

                        sessionLogger?.start(appScope)
                        inventoryManager.initialize(appScope)
                        familiarManager.initialize(appScope)
                        skillManager.initialize(appScope)
                        effectManager.initialize(appScope)
                        scriptManager.initialize()
                        questLogRequest?.syncAll()
                        moodManager?.updateFromPreferences(
                            username = charState.name,
                            activeMoodName = preferences.getString(Preferences.ACTIVE_MOOD_NAME),
                        )
                        banishManager?.load()
                        if (ascended) {
                            banishManager?.clearAvatarBanishes()
                        }
                        junkListManager?.load(preferences)
                        BuffBotDatabase.load()
                        httpClient?.let { client ->
                            appScope.launch {
                                try {
                                    BuffBotDatabase.instance.configureOfferings(client)
                                } catch (_: Exception) {
                                }
                                try {
                                    FaxBotDatabase.instance.configure(client, gameDatabase)
                                } catch (_: Exception) {
                                }
                            }
                        }
                        outfitManager?.refreshCustomOutfits()

                        inventoryManager.fetchInventory()
                        effectManager.fetchEffects()
                        if (rolloverChanged) {
                            gameRuntimeLibrary?.updateOneDesc()
                        }
                        if (charState.inTwoCrazyRandomSummer) {
                            if (TCRSDatabase.loadFromPreferences(
                                    charState.className,
                                    charState.zodiacSign,
                                    preferences,
                                )) {
                                TCRSDatabase.applyModifiers(charState.level)
                            }
                        } else {
                            TCRSDatabase.resetModifiers(preferences, charState.level)
                            TCRSDatabase.reset()
                        }
                        val sources = buildIngredientSources(charState)
                        val aggregatedCounts = ConcoctionAvailableIngredients.aggregate(sources)
                        ConcoctionDatabase.refreshConcoctionsFromAggregated(
                            ConcoctionRefreshContext.fromLiveSession(
                                aggregatedCounts = aggregatedCounts,
                                state = charState,
                                skills = skillManager.state.value.skills,
                                prefs = preferences,
                                accessibleCount = { id -> aggregatedCounts[id] ?: 0 },
                                storageCounts = sources.storage,
                            ),
                        )
                        ClanHotdogMenuCache.restoreIntoAvailability(preferences)
                        gameRuntimeLibrary?.checkDynamicModifiers()

                        // Run breakfast actions
                        breakfastManager?.runBreakfast(
                            charState = charState,
                            inventoryState = inventoryManager.state.value,
                        )

                        if (charState.inPokefam) {
                            httpClient?.let { client ->
                                try {
                                    FamTeamRequest.visit(
                                        client = client,
                                        character = character,
                                        familiarManager = familiarManager,
                                        preferences = preferences,
                                        sessionLogger = sessionLogger,
                                        inventoryManager = inventoryManager,
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        }

                        SessionState.LoggedIn
                    },
                    onFailure = { error ->
                        SessionState.Error("Character load failed: ${error.message}")
                    }
                )
            }
            is LoginResult.Failure -> SessionState.Error(loginResult.message)
            is LoginResult.Error -> SessionState.Error(loginResult.cause.message ?: "Network error")
        }
    }

    open fun logout() {
        ClanManager.clearCache(newCharacter = true)
        CharpaneValhallaSync.reset()
        PvpManager.reset()
        character.reset()
    }

    private suspend fun buildIngredientSources(charState: CharacterState): ConcoctionIngredientSources {
        val inventory = inventoryManager.state.value.items.mapValues { it.value.quantity }
        val closet = closetRequest?.fetchContents() ?: emptyMap()
        val classified = storageRequest?.fetchClassifiedContents(charState, preferences)
        val storage = if (StorageRequest.canUseStorage(charState)) {
            classified?.storage ?: emptyMap()
        } else {
            emptyMap()
        }
        val freepulls = classified?.freepulls ?: emptyMap()
        val stash = clanStashRequest?.fetchContents() ?: emptyMap()
        val display = if (charState.canUseDisplayCase) {
            displayCaseRequest?.fetchContents() ?: emptyMap()
        } else {
            emptyMap()
        }
        CollectionCacheSync.saveFromSources(preferences, closet, storage, freepulls, stash)
        CollectionCacheSync.saveDisplay(preferences, display)
        return ConcoctionIngredientSources(
            inventory = inventory,
            closet = closet,
            storage = storage,
            freepulls = freepulls,
            stash = stash,
        )
    }
}
