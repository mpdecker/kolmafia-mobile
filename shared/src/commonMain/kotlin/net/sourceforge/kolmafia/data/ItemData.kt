package net.sourceforge.kolmafia.data

data class ItemData(
    val id: Int,
    val name: String,
    val descId: String,
    val image: String,
    val primaryUse: ItemPrimaryUse,
    val secondaryUses: Set<String>,
    val access: Set<Char>,       // 'q'=quest, 'g'=gift, 't'=tradeable, 'd'=discardable
    val autosellPrice: Int,
    val plural: String?
) {
    val isTradeable get() = 't' in access
    val isDiscardable get() = 'd' in access
    val isQuestItem get() = 'q' in access
    val isEquipment get() = primaryUse.isEquipment
    val isConsumable get() = primaryUse.isConsumable
}
