package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase

/**
 * Desktop [QuestManager.handleTrickOrTreatingChange] — costume discards + Russian ice.
 */
object TrickOrTreatSync {

    const val WOLFMAN_MASK = 4260
    const val PUMPKINHEAD_MASK = 4261
    const val MUMMY_COSTUME = 4262
    const val RUSSIAN_ICE = 5073

    fun applyFromVisit(
        url: String?,
        html: String,
        equipment: Map<EquipmentSlot, String>,
        itemName: (Int) -> String = { ItemDatabase.getItemName(it) },
        clearSlot: (EquipmentSlot) -> Unit = {},
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (url != null && !url.contains("trickortreat", ignoreCase = true)) return false
        val maskId = when {
            html.contains("pull the pumpkin off of your head") -> PUMPKINHEAD_MASK
            html.contains("gick all over your mummy costume") -> MUMMY_COSTUME
            html.contains("unzipping the mask and throwing it behind you") -> WOLFMAN_MASK
            else -> null
        }
        if (maskId != null) {
            return EquipmentDiscard.discardIfEquipped(
                itemId = maskId,
                equipment = equipment,
                itemName = itemName,
                clearSlot = clearSlot,
                consumeItem = consumeItem,
            )
        }
        if (html.contains("Right on, brah. Here, have some gum.")) {
            consumeItem(RUSSIAN_ICE, 1)
            return true
        }
        return false
    }
}
