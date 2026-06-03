package net.sourceforge.kolmafia.data

data class AdventureZone(
    val zoneName: String,
    val urlParams: String,
    val locationName: String,
    val environment: String,
    val diffLevel: String,
    val statRequirement: Int,
    val goals: List<String>,
    val isOverdrunk: Boolean,
    val noWander: Boolean
) {
    val snarfblat: String? get() = urlParams.substringAfter("adventure=", "").takeIf { it.isNotBlank() }
    val isClanArea get() = urlParams.startsWith("clan_")
    val isCasino get() = urlParams.startsWith("casino=")
}
