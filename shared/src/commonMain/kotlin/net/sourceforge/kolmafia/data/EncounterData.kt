package net.sourceforge.kolmafia.data

data class EncounterData(
    val locationName: String,
    val type: String,
    val title: String
) {
    val isGlobal get() = locationName == "*"
    val isAutoStop get() = type == "STOP"
}
