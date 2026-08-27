package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.session.EncounterType

data class EncounterData(
    val locationName: String,
    val type: String,
    val title: String,
) {
    val isGlobal get() = locationName == "*"
    val encounterType: EncounterType = EncounterType.fromToken(type)
    val isAutoStop get() = encounterType.isAutostop
}
