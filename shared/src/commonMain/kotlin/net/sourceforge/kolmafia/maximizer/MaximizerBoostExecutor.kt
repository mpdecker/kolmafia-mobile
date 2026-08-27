package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.FoldItemRequest
import net.sourceforge.kolmafia.request.BoomBoxRequest
import net.sourceforge.kolmafia.request.HorseryRequest
import net.sourceforge.kolmafia.request.MindControlRequest
import net.sourceforge.kolmafia.request.ModeableRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase

/** Executes desktop-style maximizer boost cmd chains (Phase 384). */
class MaximizerBoostExecutor(
    private val gameDatabase: GameDatabase,
    private val inventoryManager: InventoryManager,
    private val equipmentRequest: EquipmentRequest,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val displayCaseRequest: DisplayCaseRequest?,
    private val clanStashRequest: ClanStashRequest?,
    private val familiarManager: FamiliarManager?,
    private val modeableRequest: ModeableRequest?,
    private val retrieveItemService: RetrieveItemService?,
    private val mallManager: MallManager?,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val cliExecutor: (suspend (String) -> Boolean)? = null,
    private val foldItemRequest: FoldItemRequest? = null,
    private val horseryRequest: HorseryRequest? = null,
    private val boomBoxRequest: BoomBoxRequest? = null,
    private val mindControlRequest: MindControlRequest? = null,
    private val skillCastRequest: SkillCastRequest? = null,
    private val uneffectRequest: UneffectRequest? = null,
) {
    suspend fun execute(cmd: String): Boolean {
        if (cmd.isBlank()) return true
        for (segment in cmd.split(';')) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue
            if (!executeSegment(trimmed)) return false
            inventoryManager.fetchInventory()
        }
        return true
    }

    private suspend fun executeSegment(segment: String): Boolean {
        val parts = segment.split(Regex("\\s+"))
        if (parts.isEmpty()) return false
        return when (parts[0].lowercase()) {
            "closet" -> executeCloset(parts)
            "stash" -> executeStash(parts)
            "display" -> executeDisplay(parts)
            "pull" -> executePull(parts)
            "make", "create" -> executeMake(parts)
            "buy" -> executeBuy(parts)
            "fold" -> executeFold(parts)
            "acquire" -> executeAcquire(parts)
            "equip" -> executeEquip(parts)
            "unequip" -> executeUnequip(parts)
            "familiar" -> executeFamiliar(parts)
            "enthrone" -> executeEnthrone(parts)
            "bjornify" -> executeBjornify(parts)
            "horsery" -> executeHorsery(parts)
            "boombox" -> executeBoombox(parts)
            "mcd" -> executeMcd(parts)
            "cast", "skill" -> executeCast(parts)
            "uneffect", "shrug" -> executeUneffect(parts)
            else -> executeModeable(segment)
        }
    }

    private fun itemIdFrom(parts: List<String>): Int? {
        val last = parts.lastOrNull() ?: return null
        val id = last.removePrefix("\u00B6").toIntOrNull()
        if (id != null) return id
        return gameDatabase.item(last)?.id
    }

    private suspend fun executeCloset(parts: List<String>): Boolean {
        if (parts.size < 4 || closetRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val ok = closetRequest.takeOut(itemId, qty).isSuccess
        if (ok) refreshClosetCache()
        return ok
    }

    private suspend fun executeStash(parts: List<String>): Boolean {
        if (parts.size < 4 || clanStashRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val ok = clanStashRequest.takeOut(itemId, qty).isSuccess
        if (ok) refreshStashCache()
        return ok
    }

    private suspend fun executeDisplay(parts: List<String>): Boolean {
        if (parts.size < 4 || displayCaseRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val ok = displayCaseRequest.takeOut(itemId, qty).isSuccess
        if (ok) refreshDisplayCache()
        return ok
    }

    private suspend fun executePull(parts: List<String>): Boolean {
        if (storageRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val ok = storageRequest.withdraw(itemId, qty).isSuccess
        if (ok) refreshStorageCache()
        return ok
    }

    private suspend fun refreshClosetCache() {
        val prefs = preferences ?: return
        val request = closetRequest ?: return
        CollectionCacheSync.refreshCloset(request, prefs)
    }

    private suspend fun refreshStorageCache() {
        val prefs = preferences ?: return
        val request = storageRequest ?: return
        CollectionCacheSync.refreshStorage(request, character?.state?.value, prefs)
    }

    private suspend fun refreshStashCache() {
        val prefs = preferences ?: return
        val request = clanStashRequest ?: return
        CollectionCacheSync.refreshStash(request, prefs)
    }

    private suspend fun refreshDisplayCache() {
        val prefs = preferences ?: return
        val request = displayCaseRequest ?: return
        CollectionCacheSync.refreshDisplay(request, prefs)
    }

    private suspend fun executeMake(parts: List<String>): Boolean {
        val itemId = itemIdFrom(parts) ?: return false
        val service = retrieveItemService ?: return false
        return service.retrieve(itemId, 1) >= 1
    }

    private suspend fun executeBuy(parts: List<String>): Boolean {
        val itemId = itemIdFrom(parts) ?: return false
        val service = retrieveItemService ?: return false
        return service.retrieve(itemId, 1) >= 1
    }

    private suspend fun executeFold(parts: List<String>): Boolean {
        val itemId = itemIdFrom(parts) ?: return false
        val folder = foldItemRequest
        if (folder != null) {
            return folder.fold(itemId).isSuccess
        }
        val service = retrieveItemService ?: return false
        return service.retrieve(itemId, 1) >= 1
    }

    private suspend fun executeAcquire(parts: List<String>): Boolean {
        val itemId = itemIdFrom(parts) ?: return false
        val service = retrieveItemService ?: return false
        if (service.retrieve(itemId, 1) >= 1) return true
        val name = gameDatabase.item(itemId)?.name ?: return false
        mallManager?.cheapestPrice(name)
        return service.retrieve(itemId, 1) >= 1
    }

    private suspend fun executeEquip(parts: List<String>): Boolean {
        if (parts.size < 3) return false
        val slotName = parts[1]
        val slot = EquipmentSlot.entries.find { it.name.equals(slotName, ignoreCase = true) }
            ?: EquipmentSlot.fromApiKey(slotName.lowercase())
            ?: return false
        val itemId = itemIdFrom(parts) ?: return false
        return equipmentRequest.equipItem(itemId, slot).isSuccess
    }

    private suspend fun executeUnequip(parts: List<String>): Boolean {
        if (parts.size < 2) return false
        val slotName = parts[1]
        val slot = EquipmentSlot.entries.find { it.name.equals(slotName, ignoreCase = true) }
            ?: EquipmentSlot.fromApiKey(slotName.lowercase())
            ?: return false
        return equipmentRequest.unequipSlot(slot).isSuccess
    }

    private suspend fun executeFamiliar(parts: List<String>): Boolean {
        if (parts.size < 2 || familiarManager == null) return false
        return familiarManager.setFamiliar(parts.drop(1).joinToString(" ")).isSuccess
    }

    private suspend fun executeEnthrone(parts: List<String>): Boolean {
        if (parts.size < 2 || familiarManager == null) return false
        return familiarManager.setEnthroned(parts.drop(1).joinToString(" ")).isSuccess
    }

    private suspend fun executeBjornify(parts: List<String>): Boolean {
        if (parts.size < 2 || familiarManager == null) return false
        return familiarManager.setBjornified(parts.drop(1).joinToString(" ")).isSuccess
    }

    private suspend fun executeHorsery(parts: List<String>): Boolean {
        val request = horseryRequest
        if (request == null) return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        val horse = parts.drop(1).joinToString(" ").ifBlank { return false }
        return request.ride(horse).isSuccess
    }

    private suspend fun executeBoombox(parts: List<String>): Boolean {
        val request = boomBoxRequest
        if (request == null) return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        val song = parts.drop(1).joinToString(" ").ifBlank { return false }
        return request.play(song).isSuccess
    }

    private suspend fun executeMcd(parts: List<String>): Boolean {
        val request = mindControlRequest
        if (request == null) return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        val level = parts.getOrNull(1)?.toIntOrNull() ?: return false
        return request.setLevel(level).isSuccess
    }

    private suspend fun executeCast(parts: List<String>): Boolean {
        val request = skillCastRequest
        if (request == null) return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        val skillName = parts.drop(1).joinToString(" ").ifBlank { return false }
        val skillId = SkillDefinitionDatabase.getByName(skillName)?.id
            ?: return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        return request.cast(skillId).isSuccess
    }

    private suspend fun executeUneffect(parts: List<String>): Boolean {
        val request = uneffectRequest
        if (request == null) return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        val effectName = parts.drop(1).joinToString(" ").ifBlank { return false }
        val effectId = EffectDatabase.getByName(effectName)?.id
            ?: return cliExecutor?.invoke(parts.joinToString(" ")) ?: false
        return request.uneffect(effectId).isSuccess
    }

    private suspend fun executeModeable(segment: String): Boolean {
        val space = segment.indexOf(' ')
        if (space > 0) {
            val command = segment.substring(0, space)
            val modeable = Modeable.entries.find { it.command.equals(command, ignoreCase = true) }
            if (modeable != null) {
                val request = modeableRequest ?: return true
                val mode = segment.substring(space + 1).trim()
                return request.setMode(modeable, mode).isSuccess
            }
        }
        return cliExecutor?.invoke(segment) ?: false
    }
}
