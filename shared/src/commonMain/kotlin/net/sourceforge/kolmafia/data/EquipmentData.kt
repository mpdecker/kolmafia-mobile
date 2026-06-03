package net.sourceforge.kolmafia.data

data class EquipmentData(
    val name: String,
    val power: Int,
    val statRequirement: String?   // e.g. "Mox: 85", "Mus: 40", null means "none"
) {
    val requiresStat: Boolean get() = statRequirement != null
}
