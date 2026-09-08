package net.sourceforge.kolmafia.data

/**
 * One combat-table row from combats.txt.
 *
 * [weight] may be negative (ultra-rare / special). [rejectionPercent] is the `rN` flag.
 * [ascensionParity]: 0 = any, 1 = odd-only (`o`), 2 = even-only (`e`).
 * [superlikely] is true when [EncounterManager] classifies the monster as SUPERLIKELY.
 */
data class MonsterWeight(
    val name: String,
    val weight: Int,
    val rejectionPercent: Int = 0,
    val ascensionParity: Int = 0,
    val superlikely: Boolean = false,
)

data class ZoneCombatData(
    val locationName: String,
    val combatPercent: Int,
    val monsters: List<MonsterWeight>,
)
