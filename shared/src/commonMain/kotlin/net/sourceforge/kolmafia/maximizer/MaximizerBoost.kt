package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot

/** Desktop [net.sourceforge.kolmafia.maximizer.Boost] equipment subset for emitSlot output. */
data class MaximizerBoost(
    val cmd: String,
    val text: String,
    val slot: EquipmentSlot? = null,
    val itemId: Int = 0,
    val itemName: String = "",
    val delta: Double = 0.0,
    val isEquipment: Boolean = true,
    val familiarRace: String? = null,
) : Comparable<MaximizerBoost> {
    override fun compareTo(other: MaximizerBoost): Int {
        if (isEquipment != other.isEquipment) return if (isEquipment) -1 else 1
        if (isEquipment) return 0
        return other.delta.compareTo(delta)
    }
}

enum class MaximizerEquipScope {
    SPECULATE,
    EQUIP_NOW,
    ;

    companion object {
        /** Desktop EquipScope.byIndex mapping for ASH 5-arg maximize. */
        fun byIndex(index: Int): MaximizerEquipScope = when (index) {
            -1 -> EQUIP_NOW
            else -> SPECULATE
        }
    }
}
