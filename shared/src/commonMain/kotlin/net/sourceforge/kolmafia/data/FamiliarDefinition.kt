package net.sourceforge.kolmafia.data

data class FamiliarDefinition(
    val id: Int,
    val name: String,
    val image: String,
    val types: Set<String>,         // "combat0", "meat0", "block", etc.
    val larvaItem: String,
    val hatchlingItem: String,
    val arenaCombatMoves: Int,      // cm
    val arenaStrength: Int,         // sh
    val arenaOc: Int,               // oc
    val arenaHs: Int,               // hs
    val attributes: Set<String>     // "sentient", "organic", "haswings", etc.
) {
    val isPhysicalAttacker get() = "combat0" in types
    val isElementalAttacker get() = "combat1" in types
    val isMeatDropper get() = "meat0" in types
    val isStatGainer get() = "stat0" in types || "stat1" in types
    val isItemDropper get() = "item0" in types || "item1" in types || "item2" in types
    val canBreathUnderwater get() = "underwater" in attributes
}
