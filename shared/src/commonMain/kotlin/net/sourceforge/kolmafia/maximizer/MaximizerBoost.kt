package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot

/** Desktop [net.sourceforge.kolmafia.maximizer.Boost] for emitSlot / execute output. */
data class MaximizerBoost(
    val cmd: String,
    val text: String,
    val slot: EquipmentSlot? = null,
    val itemId: Int = 0,
    val itemName: String = "",
    val delta: Double = 0.0,
    val isEquipment: Boolean = true,
    val familiarRace: String? = null,
    val priority: Boolean = false,
    /** Effect name for cast/uneffect boosts. */
    val effectName: String? = null,
    /** True when this boost shrugs/removes an effect. */
    val isShrug: Boolean = false,
    /** Horsery horse name when cmd is horsery. */
    val horseName: String? = null,
    /** Modeable command payload when applicable. */
    val modeableMode: String? = null,
) : Comparable<MaximizerBoost> {
    override fun compareTo(other: MaximizerBoost): Int {
        if (isEquipment != other.isEquipment) return if (isEquipment) -1 else 1
        if (priority != other.priority) return if (priority) -1 else 1
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
