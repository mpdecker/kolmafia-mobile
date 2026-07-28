package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Crimbo23*Request [accessible] gates for elf/pirate coinmaster shops. */
object Crimbo23ShopAccessibility {

    const val KELFLAR_VEST = 11440
    const val CRIMBUCCANEER_SHIRT = 11407
    const val ELF_ARMY_MACHINE_PARTS = 11402
    const val CRIMBUCCANEER_FLOTSAM = 11405

    private enum class Side { ELF, PIRATE }

    private enum class Zone(val locationName: String, val controlPref: String) {
        ARMORY("armory", "crimbo23ArmoryControl"),
        BAR("bar", "crimbo23BarControl"),
        CAFE("cafe", "crimbo23CafeControl"),
        FACTORY("factory", "crimbo23FoundryControl"),
    }

    private data class ShopGate(
        val shopId: String,
        val side: Side,
        val zone: Zone,
        val closedMessage: String = "CrimboTown is closed",
    )

    private val SHOP_GATES = listOf(
        ShopGate("crimbo23_elf_armory", Side.ELF, Zone.ARMORY),
        ShopGate("crimbo23_pirate_armory", Side.PIRATE, Zone.ARMORY),
        ShopGate("crimbo23_elf_bar", Side.ELF, Zone.BAR),
        ShopGate("crimbo23_pirate_bar", Side.PIRATE, Zone.BAR, closedMessage = "CrimboTown 2023 is closed"),
        ShopGate("crimbo23_elf_cafe", Side.ELF, Zone.CAFE),
        ShopGate("crimbo23_pirate_cafe", Side.PIRATE, Zone.CAFE),
        ShopGate("crimbo23_elf_factory", Side.ELF, Zone.FACTORY),
        ShopGate("crimbo23_pirate_factory", Side.PIRATE, Zone.FACTORY),
    )

    fun inaccessibleReason(shopId: String, prefs: Preferences?): String? {
        val gate = SHOP_GATES.firstOrNull { it.shopId.equals(shopId, ignoreCase = true) } ?: return null
        return reasonForControl(gate, prefs?.getString(gate.zone.controlPref, "none") ?: "none")
    }

    private fun reasonForControl(gate: ShopGate, control: String): String? =
        when (control) {
            "none" -> gate.closedMessage
            "elf" -> when (gate.side) {
                Side.ELF -> null
                Side.PIRATE -> "The elves control the ${gate.zone.locationName}"
            }
            "pirate" -> when (gate.side) {
                Side.PIRATE -> null
                Side.ELF -> "The pirates control the ${gate.zone.locationName}"
            }
            "contested" ->
                "The elves and pirates are fighting for control of the ${gate.zone.locationName}"
            else -> null
        }
}
