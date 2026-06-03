package net.sourceforge.kolmafia.data

data class MonsterWeight(val name: String, val weight: Int)

data class ZoneCombatData(
    val locationName: String,
    val combatPercent: Int,
    val monsters: List<MonsterWeight>
)
