package net.sourceforge.kolmafia.session

import kotlinx.coroutines.coroutineScope
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.GuildRequest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.skill.SkillManager

/** Headless orchestration for opening a standard-class guild. */
class GuildUnlockManager(
    private val request: GuildRequest,
    private val adventureManager: AdventureManager,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val questDatabase: QuestDatabase,
    private val inventoryManager: InventoryManager,
    private val equipmentManager: EquipmentManager? = null,
    private val equipmentRequest: EquipmentRequest? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val skillManager: SkillManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    data class UnlockPlan(
        val mainStat: net.sourceforge.kolmafia.character.MainStat,
        val location: AdventureLocation,
        val challengeChoice: Int,
        val targetItemId: Int?,
        val quest: Quest,
        val pantsItemId: Int? = null,
    )

    sealed interface UnlockResult {
        data object AlreadyUnlocked : UnlockResult
        data class Unlocked(val plan: UnlockPlan) : UnlockResult
    }

    suspend fun unlockGuild(): Result<UnlockResult> {
        val state = character.state.value
        if (!canUnlockGuild(state)) {
            return Result.failure(
                IllegalStateException("You don't have a guild available to open. No guild for you."),
            )
        }
        if (guildStoreAvailable(state, preferences)) {
            return Result.success(UnlockResult.AlreadyUnlocked)
        }

        request.visit("challenge").getOrElse { return Result.failure(it) }
        if (guildStoreAvailable(character.state.value, preferences)) {
            return Result.success(UnlockResult.AlreadyUnlocked)
        }

        val plan = planFor(character.state.value)
            ?: return Result.failure(IllegalStateException("Unable to determine your guild challenge."))
        if (plan.mainStat == net.sourceforge.kolmafia.character.MainStat.MOXIE &&
            plan.targetItemId == null
        ) {
            return Result.failure(IllegalStateException("Put on some pants and try again."))
        }
        val setting = "choiceAdventure${plan.challengeChoice}"
        val previousChoice = preferences.getInt(setting, 0)
        val pants = plan.pantsItemId
        val initialCount = plan.targetItemId?.let(inventoryManager::getCount) ?: 0

        preferences.setInt(setting, 1)
        val stopResult = try {
            coroutineScope {
                adventureManager.runUntilItem(
                    location = plan.location,
                    itemId = plan.targetItemId ?: -1,
                    initialCount = initialCount,
                    maxTurns = character.state.value.adventuresLeft,
                    scope = this,
                )
            }
        } finally {
            preferences.setInt(setting, previousChoice)
            restorePants(pants)
        }

        val acquired = when (stopResult) {
            is AdventureManager.ItemStopResult.Acquired -> true
            is AdventureManager.ItemStopResult.AlreadyPresent -> true
            is AdventureManager.ItemStopResult.Stopped -> false
        }
        if (!acquired) {
            return Result.failure(
                IllegalStateException("Guild was not unlocked: ${stopResult.message}"),
            )
        }

        request.visit("challenge").getOrElse { return Result.failure(it) }
        request.visit("paco").getOrElse { return Result.failure(it) }
        return if (guildStoreAvailable(character.state.value, preferences) ||
            questDatabase.isQuestFinished(plan.quest)
        ) {
            sessionLogger?.appendRawLine("Guild successfully unlocked")
            Result.success(UnlockResult.Unlocked(plan))
        } else {
            Result.failure(IllegalStateException("Guild was not unlocked."))
        }
    }

    private suspend fun restorePants(itemId: Int?) {
        if (itemId == null || itemId <= 0) return
        val manager = equipmentManager ?: return
        if (manager.hasEquipped(itemId)) return
        retrieveItemService?.retrieve(itemId, 1)
        equipmentRequest?.equipItem(itemId, EquipmentSlot.PANTS)
    }

    companion object {
        fun canUnlockGuild(state: CharacterState): Boolean =
            state.characterClassEnum.isStandardClass &&
                !state.inPokefam &&
                !state.inRobocore

        fun guildStoreAvailable(state: CharacterState, preferences: Preferences?): Boolean =
            canUnlockGuild(state) &&
                !state.inNuclearAutumn &&
                preferences?.getInt("lastGuildStoreOpen", -1) == state.ascensionNumber

        fun planFor(state: CharacterState): UnlockPlan? {
            if (!canUnlockGuild(state)) return null
            fun location(id: String, fallbackName: String, zone: String): AdventureLocation =
                AdventureDatabase.getBySnarfblat(id)?.toLocation()
                    ?: AdventureLocation(id = id, name = fallbackName, zone = zone)
            return when (state.mainStat) {
                net.sourceforge.kolmafia.character.MainStat.MUSCLE -> UnlockPlan(
                    mainStat = state.mainStat,
                    location = location("114", "The Outskirts of Cobb's Knob", "Cobb's Knob"),
                    challengeChoice = 543,
                    targetItemId = 5193,
                    quest = Quest.MUSCLE,
                )
                net.sourceforge.kolmafia.character.MainStat.MYSTICALITY -> UnlockPlan(
                    mainStat = state.mainStat,
                    location = location("113", "The Haunted Pantry", "The Spooky Forest"),
                    challengeChoice = 544,
                    targetItemId = 5194,
                    quest = Quest.MYST,
                )
                net.sourceforge.kolmafia.character.MainStat.MOXIE -> {
                    val pantsName = state.equipment[EquipmentSlot.PANTS].orEmpty()
                    val pantsId = net.sourceforge.kolmafia.data.ItemDatabase.getByName(pantsName)?.id
                    UnlockPlan(
                        mainStat = state.mainStat,
                        location = location("112", "The Sleazy Back Alley", "The Bigg's"),
                        challengeChoice = 542,
                    targetItemId = pantsId,
                        quest = Quest.MOXIE,
                        pantsItemId = pantsId,
                    )
                }
            }
        }
    }
}
