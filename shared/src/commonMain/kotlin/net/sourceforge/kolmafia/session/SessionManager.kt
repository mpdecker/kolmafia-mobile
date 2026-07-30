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
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.TCRSDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.LoginResult
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.shop.ShopRowDatabase
import net.sourceforge.kolmafia.skill.SkillManager

sealed class SessionState {
    object LoggedOut : SessionState()
    object LoggedIn : SessionState()
    data class Error(val message: String) : SessionState()
}

class SessionManager(
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
) {
    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun login(username: String, password: String): SessionState {
        return when (val loginResult = loginRequest.login(username, password)) {
            is LoginResult.Success -> {
                preferences.setString(Preferences.LAST_USERNAME, username)
                gameDatabase.load()
                ShopRowDatabase.restoreLearnedRows(preferences)
                characterRequest.fetchCharacterState().fold(
                    onSuccess = { apiResponse ->
                        character.updateFromApiResponse(apiResponse)
                        val charState = character.state.value
                        dailyResourceTracker.syncDay(charState.dayCount)

                        // Gate rollover clear on actual day change
                        val lastDay = preferences.getInt(Preferences.LAST_DAYCOUNT, -1)
                        val dayChanged = charState.dayCount != lastDay
                        if (dayChanged) {
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
                        moodManager?.loadMoodLibrary()
                        moodManager?.loadActiveMood()
                        banishManager?.load()
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
                        if (dayChanged) {
                            gameRuntimeLibrary?.updateOneDesc()
                        }
                        if (charState.inTwoCrazyRandomSummer) {
                            TCRSDatabase.loadFromPreferences(
                                charState.className,
                                charState.zodiacSign,
                                preferences,
                            )
                        } else {
                            TCRSDatabase.reset()
                        }
                        gameRuntimeLibrary?.checkDynamicModifiers()

                        // Run breakfast actions
                        breakfastManager?.runBreakfast(
                            charState = charState,
                            inventoryState = inventoryManager.state.value,
                        )

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

    fun logout() {
        character.reset()
    }
}
