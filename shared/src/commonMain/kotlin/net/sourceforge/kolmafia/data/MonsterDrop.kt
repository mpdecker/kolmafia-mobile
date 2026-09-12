package net.sourceforge.kolmafia.data

/**
 * Monster item drop entry. Mirrors desktop [MonsterDrop]:
 * rates may be fractional (e.g. `c0.1`); multi-drops (`m`) carry an [itemCount] range.
 */
data class MonsterDrop(
    val itemName: String,
    val dropRate: Double, // 0–100; may be fractional
    val prefix: Char?, // 'p','n','c','f','m','a' or null (unknown-rate uses null + 0.0)
    val itemCount: String = "", // multi-drop quantity text, e.g. "1-5" / "2"
)
