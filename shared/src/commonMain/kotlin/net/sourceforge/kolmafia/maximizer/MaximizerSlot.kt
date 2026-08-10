package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot

/**
 * Equipment slots used by the Maximizer item-ranking pipeline, including desktop pseudo-slots
 * for 1H weapons and dual-wield aux routing. Pseudo slots are distinct from real [EquipmentSlot.ACC2]/[EquipmentSlot.ACC3].
 */
enum class MaximizerSlot {
    HAT,
    WEAPON,
    OFFHAND,
    SHIRT,
    PANTS,
    ACC1,
    ACC2,
    ACC3,
    FAMILIAR,
    CONTAINER,
    /** Desktop Evaluator.WEAPON_1H — one-handed weapons scored separately from 2H. */
    WEAPON_1H,
    /** Desktop Evaluator.OFFHAND_MELEE — 1H melee weapons eligible as offhand when dual-wielding. */
    OFFHAND_MELEE,
    /** Desktop Evaluator.OFFHAND_RANGED — sixguns eligible as offhand when dual-wielding. */
    OFFHAND_RANGED,
    ;

    val isPseudo: Boolean
        get() = this in PSEUDO_SLOTS

    fun toEquipmentSlot(): EquipmentSlot? = when (this) {
        HAT -> EquipmentSlot.HAT
        WEAPON, WEAPON_1H -> EquipmentSlot.WEAPON
        OFFHAND, OFFHAND_MELEE, OFFHAND_RANGED -> EquipmentSlot.OFFHAND
        SHIRT -> EquipmentSlot.SHIRT
        PANTS -> EquipmentSlot.PANTS
        ACC1 -> EquipmentSlot.ACC1
        ACC2 -> EquipmentSlot.ACC2
        ACC3 -> EquipmentSlot.ACC3
        FAMILIAR -> EquipmentSlot.FAMILIAR
        CONTAINER -> EquipmentSlot.CONTAINER
    }

    companion object {
        private val PSEUDO_SLOTS = setOf(WEAPON_1H, OFFHAND_MELEE, OFFHAND_RANGED)

        fun fromEquipmentSlot(slot: EquipmentSlot): MaximizerSlot? = when (slot) {
            EquipmentSlot.HAT -> HAT
            EquipmentSlot.WEAPON -> WEAPON
            EquipmentSlot.OFFHAND -> OFFHAND
            EquipmentSlot.SHIRT -> SHIRT
            EquipmentSlot.PANTS -> PANTS
            EquipmentSlot.ACC1 -> ACC1
            EquipmentSlot.ACC2 -> ACC2
            EquipmentSlot.ACC3 -> ACC3
            EquipmentSlot.FAMILIAR -> FAMILIAR
            EquipmentSlot.CONTAINER -> CONTAINER
            EquipmentSlot.CODPIECE1, EquipmentSlot.CODPIECE2, EquipmentSlot.CODPIECE3,
            EquipmentSlot.CODPIECE4, EquipmentSlot.CODPIECE5,
            -> null
        }

        fun weaponBuckets(spec: MaximizeSpec): List<MaximizerSlot> =
            if (spec.requireHands) listOf(WEAPON, WEAPON_1H) else listOf(WEAPON)

        /** Buckets consulted when equipping/searching the weapon slot (includes 1H always). */
        fun weaponSearchSlots(): List<MaximizerSlot> = listOf(WEAPON, WEAPON_1H)

        fun offhandBuckets(spec: MaximizeSpec): List<MaximizerSlot> =
            if (spec.requireHands) {
                listOf(OFFHAND, OFFHAND_MELEE, OFFHAND_RANGED)
            } else {
                listOf(OFFHAND)
            }

        fun slotsForEquipmentSlot(slot: EquipmentSlot, spec: MaximizeSpec): List<MaximizerSlot> =
            when (slot) {
                EquipmentSlot.WEAPON -> weaponSearchSlots()
                EquipmentSlot.OFFHAND -> offhandBuckets(spec)
                EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3 -> listOf(ACC1)
                else -> fromEquipmentSlot(slot)?.let { listOf(it) }.orEmpty()
            }
    }
}
