package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase

/**
 * Desktop [QuestManager.handleSneakyPeteChange] — overdrunk adventure discards.
 */
object SneakyPeteDiscardSync {

    const val NOVELTY_BUTTON = 2072
    const val TATTERED_PAPER_CROWN = 3231

    fun applyFromAdventure(
        html: String,
        inebriety: Int,
        equipment: Map<EquipmentSlot, String>,
        itemName: (Int) -> String = { ItemDatabase.getItemName(it) },
        clearSlot: (EquipmentSlot) -> Unit = {},
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (inebriety <= 25) return false
        val itemId = when {
            html.contains("You hand him your button and take his glowstick") -> NOVELTY_BUTTON
            html.contains("Ah, man, you dropped your crown back there!") -> TATTERED_PAPER_CROWN
            else -> return false
        }
        return EquipmentDiscard.discardIfEquipped(
            itemId = itemId,
            equipment = equipment,
            itemName = itemName,
            clearSlot = clearSlot,
            consumeItem = consumeItem,
        )
    }
}
