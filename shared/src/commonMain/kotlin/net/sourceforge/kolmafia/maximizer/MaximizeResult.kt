package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot

data class MaximizeResult(
    val success: Boolean,
    val goal: String,
    val scoreBefore: Double,
    val scoreAfter: Double,
    val equipped: Map<EquipmentSlot, String> = emptyMap(),
    val familiarSwitched: String? = null,
    val enthronedSwitched: String? = null,
    val bjornifiedSwitched: String? = null,
    val thrallSwitched: String? = null,
)
