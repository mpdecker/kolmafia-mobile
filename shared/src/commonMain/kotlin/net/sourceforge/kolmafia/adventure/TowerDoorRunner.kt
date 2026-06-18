package net.sourceforge.kolmafia.adventure

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.skill.SkillCastRequest

internal class TowerDoorRunner(
    private val httpClient: HttpClient,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val questDatabase: QuestDatabase,
    private val inventoryManager: InventoryManager?,
    private val retrieveItemService: RetrieveItemService?,
    private val gameDatabase: GameDatabase?,
    private val skillCastRequest: SkillCastRequest?,
    private val choiceRequest: ChoiceRequest?,
    private val coinmasterManager: CoinmasterManager?,
    private val skillManager: net.sourceforge.kolmafia.skill.SkillManager?,
    private val processQuestHooks: (html: String, url: String) -> Unit,
) {
    fun run(print: (String) -> Unit): Boolean = runBlocking {
        runInternal(print)
    }

    suspend fun runInternal(print: (String) -> Unit): Boolean {
        val status = questDatabase.getProgress(Quest.FINAL)
        if (status != "step5") {
            print(TowerDoorConfig.towerDoorErrorMessage(status, questDatabase))
            return false
        }

        val charState = character.state.value
        val locks = TowerDoorConfig.locksFor(charState)
        val doorPlace = TowerDoorConfig.doorPlaceFor(charState)
        val doorPath = "place.php?whichplace=$doorPlace"
        val doorHtml = fetchPage(doorPath) ?: return false
        processQuestHooks(doorHtml, "$KOL_BASE_URL/$doorPath")

        val doorknob = locks.lastOrNull { it.isDoorknob } ?: return false
        var needed = TowerDoorConfig.neededLocks(preferences, locks)

        if (needed.isNotEmpty()) {
            for (lock in needed) {
                if (!retrieveKey(lock, print)) return false
            }
            for (lock in needed) {
                val unlockPath = "$doorPath&action=${lock.action}"
                val unlockHtml = fetchPage(unlockPath) ?: return false
                processQuestHooks(unlockHtml, "$KOL_BASE_URL/$unlockPath")
                if (!TowerDoorConfig.isKeyUsed(preferences, lock.keyName)) {
                    print("Failed to open ${lock.name} using ${lock.keyName}")
                    return false
                }
            }
        }

        val knobPath = "$doorPath&action=${doorknob.action}"
        val knobHtml = fetchPage(knobPath) ?: return false
        processQuestHooks(knobHtml, "$KOL_BASE_URL/$knobPath")

        if (questDatabase.getProgress(Quest.FINAL) == "step6") {
            print("Tower Door open!")
            return true
        }
        print("Tower Door not open. Unexpected quest status: ${Quest.FINAL.prefKey} = ${questDatabase.getProgress(Quest.FINAL)}")
        return false
    }

    private suspend fun fetchPage(path: String): String? {
        return try {
            val url = "$KOL_BASE_URL/$path"
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) return null
            response.bodyAsText()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun retrieveKey(lock: TowerDoorLock, print: (String) -> Unit): Boolean {
        if (lock.isDoorknob) return true
        val itemId = lock.keyItemId ?: return true
        if (inventoryCount(itemId) > 0) return true
        if (isKeyEquipped(itemId)) {
            val retrieved = retrieveItemService?.retrieve(itemId, 1) ?: 0
            if (retrieved >= 1) return true
        }

        if (lock.requiresAdventure) {
            print(TowerDoorConfig.adventureKeyErrorMessage(lock))
            return false
        }

        if (tryLockPick(lock, print)) return true

        if (lock.special && character.state.value.isKingdomOfExploathing) {
            val master = coinmasterManager?.resolveMaster("Cosmic Ray's Bazaar")
            if (master != null && coinmasterManager.buy(master, itemId, 1) >= 1) {
                if (inventoryCount(itemId) > 0) return true
            }
            val keyLabel = gameDatabase?.item(itemId)?.name ?: lock.keyName
            print("Failed to acquire $keyLabel")
            return false
        }

        val retrieved = retrieveItemService?.retrieve(itemId, 1) ?: 0
        if (retrieved >= 1) return true
        val keyLabel = gameDatabase?.item(itemId)?.name ?: lock.keyName
        print("Failed to create $keyLabel")
        return false
    }

    private suspend fun tryLockPick(lock: TowerDoorLock, print: (String) -> Unit): Boolean {
        val option = TowerDoorConfig.legendKeyPickOption(lock.keyItemId) ?: return false
        if (preferences.getBoolean("lockPicked", false)) return false
        val hasSkill = skillManager?.state?.value?.skills?.any { it.id == TowerDoorConfig.LOCK_PICKING_SKILL } == true
        if (!hasSkill) return false
        val caster = skillCastRequest ?: return false
        val chooser = choiceRequest ?: return false
        val previousChoice = preferences.getInt("choiceAdventure1414", 0)
        try {
            preferences.setInt("choiceAdventure1414", option)
            if (caster.cast(TowerDoorConfig.LOCK_PICKING_SKILL).isFailure) return false
            if (chooser.choose(TowerDoorConfig.LOCK_PICKING_CHOICE, option).isFailure) return false
            val itemId = lock.keyItemId ?: return false
            if (inventoryCount(itemId) <= 0) {
                print("Failed to pick ${gameDatabase?.item(itemId)?.name ?: lock.keyName}")
                return false
            }
            return true
        } finally {
            preferences.setInt("choiceAdventure1414", previousChoice)
        }
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    private fun isKeyEquipped(itemId: Int): Boolean {
        val itemName = gameDatabase?.item(itemId)?.name ?: return false
        return character.state.value.equipment.values.any {
            it.equals(itemName, ignoreCase = true)
        }
    }
}

internal fun GameRuntimeLibrary.runTowerDoor(print: (String) -> Unit): Boolean {
    val client = httpClient ?: return false
    val char = character ?: return false
    val prefs = preferences ?: return false
    val db = questDatabase ?: return false
    return runBlocking {
        TowerDoorRunner(
            httpClient = client,
            character = char,
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventoryManager,
            retrieveItemService = retrieveItemService,
            gameDatabase = gameDatabase,
            skillCastRequest = client.let { SkillCastRequest(it) },
            choiceRequest = choiceRequest,
            coinmasterManager = coinmasterManager,
            skillManager = skillManager,
            processQuestHooks = { html, url -> processVisitQuestHooks(html, url) },
        ).runInternal(print)
    }
}
