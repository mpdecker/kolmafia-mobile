package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.ModeableRequest
import net.sourceforge.kolmafia.request.StorageRequest

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
    private val cliExecutor: (suspend (String) -> Boolean)? = null,
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
        return closetRequest.takeOut(itemId, qty).isSuccess
    }

    private suspend fun executeStash(parts: List<String>): Boolean {
        if (parts.size < 4 || clanStashRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return clanStashRequest.takeOut(itemId, qty).isSuccess
    }

    private suspend fun executeDisplay(parts: List<String>): Boolean {
        if (parts.size < 4 || displayCaseRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return displayCaseRequest.takeOut(itemId, qty).isSuccess
    }

    private suspend fun executePull(parts: List<String>): Boolean {
        if (storageRequest == null) return false
        val itemId = itemIdFrom(parts) ?: return false
        val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
        return storageRequest.withdraw(itemId, qty).isSuccess
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
