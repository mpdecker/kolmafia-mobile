package net.sourceforge.kolmafia.equipment

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.request.EquipmentRequest

class OutfitCheckpoint private constructor(
    private val snapshot: Map<EquipmentSlot, String?>,
    private val equipmentRequest: EquipmentRequest,
    private val gameDatabase: GameDatabase,
) {
    suspend fun restore() {
        for (slot in EquipmentSlot.entries) {
            val itemName = snapshot[slot]
            if (itemName.isNullOrBlank()) {
                equipmentRequest.unequipSlot(slot)
            } else {
                val itemId = gameDatabase.item(itemName)?.id ?: continue
                equipmentRequest.equipItem(itemId, slot)
            }
        }
    }

    suspend inline fun <T> use(block: suspend () -> T): T {
        try {
            return block()
        } finally {
            restore()
        }
    }

    companion object {
        private var saved: OutfitCheckpoint? = null

        suspend fun snapshot(
            character: KoLCharacter,
            equipmentRequest: EquipmentRequest,
            gameDatabase: GameDatabase,
        ): OutfitCheckpoint {
            val snap = EquipmentSlot.entries.associateWith { slot ->
                character.state.value.equipment[slot]
            }
            return OutfitCheckpoint(snap, equipmentRequest, gameDatabase).also { saved = it }
        }

        suspend fun restoreSaved(
            equipmentRequest: EquipmentRequest,
            gameDatabase: GameDatabase,
        ): Boolean {
            val checkpoint = saved ?: return false
            checkpoint.restore()
            return true
        }

        fun clearSaved() {
            saved = null
        }

        /** Desktop [SpecialOutfit.forgetEquipment] — drop consumed gear from saved checkpoint. */
        fun forgetEquipment(itemId: Int, gameDatabase: GameDatabase) {
            val itemName = gameDatabase.item(itemId)?.name ?: return
            val checkpoint = saved ?: return
            saved = checkpoint.withoutEquipmentNamed(itemName)
        }
    }

    private fun withoutEquipmentNamed(itemName: String): OutfitCheckpoint {
        val updated = snapshot.mapValues { (_, equipped) ->
            if (equipped.equals(itemName, ignoreCase = true)) null else equipped
        }
        return OutfitCheckpoint(updated, equipmentRequest, gameDatabase)
    }
}
