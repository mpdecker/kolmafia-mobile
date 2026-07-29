package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [CleanupJunkRequest.cleanup] orchestration for junk list items.
 */
class CleanupJunkRunner(
    private val junkListManager: JunkListManager,
    private val inventoryManager: InventoryManager,
    private val untinkerRequest: UntinkerRequest,
    private val pulverizeRequest: PulverizeRequest,
    private val useItemRequest: UseItemRequest,
    private val autosellRequest: AutosellRequest,
    private val skillManager: SkillManager,
    private val character: KoLCharacter,
    private val gameDatabase: GameDatabase,
    private val closetRequest: ClosetRequest? = null,
) {

    suspend fun cleanup() {
        val items = junkListManager.itemIds()
        val charState = character.state.value

        val closet = closetRequest
        if (closet != null) {
            val closetContents = closet.fetchContents()
            for (itemId in items) {
                if (!junkListManager.isSingleton(itemId)) continue
                if (inventoryCount(itemId) <= 0) continue
                if ((closetContents[itemId] ?: 0) > 0) continue
                closet.putIn(itemId, 1)
            }
        }

        val canUntinker = untinkerRequest.canUntinker()
        do {
            var madeUntinkerRequest = false
            for (itemId in items) {
                val qty = inventoryCount(itemId)
                if (qty <= 0) continue

                val itemName = gameDatabase.item(itemId)?.name ?: continue

                if (canUntinker && ConcoctionDatabase.getByResult(itemName)?.isCombining == true) {
                    untinkerRequest.untinker(itemId, qty)
                    madeUntinkerRequest = true
                    continue
                }

                if (itemId in AUTO_USE_BOX_IDS) {
                    useItemRequest.use(itemId, qty)
                }
            }
        } while (madeUntinkerRequest)

        if (skillManager.state.value.skills.any { it.id == PULVERIZE_SKILL_ID }) {
            val hasHammer = inventoryCount(PulverizeRequest.TENDER_HAMMER) > 0
            val hasMalusAccess =
                charState.characterClassEnum.isMuscleBased && !charState.isAxecore

            for (itemId in items) {
                if (junkListManager.isMemento(itemId)) continue
                val itemData = gameDatabase.item(itemId) ?: continue
                val itemName = itemData.name
                if (itemName.startsWith("antique", ignoreCase = true)) continue

                val qty = inventoryCount(itemId)
                if (qty <= 0) continue
                if (NpcStoreDatabase.containsItem(itemId, validate = false)) continue

                val power = EquipmentDatabase.getPower(itemId)
                when (itemData.primaryUse) {
                    ItemPrimaryUse.HAT,
                    ItemPrimaryUse.PANTS,
                    ItemPrimaryUse.SHIRT,
                    ItemPrimaryUse.WEAPON,
                    ItemPrimaryUse.SIXGUN,
                    ItemPrimaryUse.OFFHAND,
                        -> {
                        if (hasHammer && (power >= 100 || (hasMalusAccess && power > 10))) {
                            pulverizeRequest.pulverize(itemId, qty)
                        }
                    }

                    ItemPrimaryUse.ACCESSORY,
                    ItemPrimaryUse.FAMILIAR,
                        -> {
                        if (hasHammer) {
                            pulverizeRequest.pulverize(itemId, qty)
                        }
                    }

                    else -> {
                        if (itemName.endsWith("powder", ignoreCase = true) ||
                            itemName.endsWith("nuggets", ignoreCase = true)
                        ) {
                            pulverizeRequest.pulverize(itemId, qty)
                        }
                    }
                }
            }
        }

        val canInteract = StoragePullRules.canInteract(charState)
        val meatPasteId = gameDatabase.item("meat paste")?.id

        for (itemId in items) {
            if (junkListManager.isMemento(itemId)) continue
            if (itemId == meatPasteId) continue
            val qty = inventoryCount(itemId)
            if (qty <= 0) continue

            val sellQty = if (canInteract) qty else (qty - 1).coerceAtLeast(0)
            if (sellQty > 0) {
                autosellRequest.autosell(itemId, sellQty)
            }
        }
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager.state.value.items[itemId]?.quantity ?: 0

    companion object {
        private const val PULVERIZE_SKILL_ID = 1016

        private val AUTO_USE_BOX_IDS = setOf(
            184,   // briefcase
            533,   // Gnollish toolbox
            553,   // 31337 scroll
            604,   // Penultimate Fantasy chest
            621,   // Warm Subject gift certificate
            831,   // small box
            832,   // large box
            1768,  // Gnomish toolbox
            1917,  // old leather wallet
            1918,  // old coin purse
            2057,  // black pension check
            2058,  // black picnic basket
            2511,  // Frat Army FGF
            2512,  // Hippy Army MPE
            2536,  // canopic jar
            2612,  // ancient vinyl coin purse
        )
    }
}
