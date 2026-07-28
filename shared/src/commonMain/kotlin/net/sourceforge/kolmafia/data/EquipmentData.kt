package net.sourceforge.kolmafia.data

data class EquipmentData(
    val name: String,
    val power: Int,
    val statRequirement: String? = null,   // e.g. "Mox: 85", "Mus: 40", null means "none"
    val hands: Int = 0,
    val itemType: String? = null,
) {
    val requiresStat: Boolean get() = statRequirement != null
}

enum class WeaponStat {
    NONE,
    MUSCLE,
    MYSTICALITY,
    MOXIE,
}
