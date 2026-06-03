package net.sourceforge.kolmafia.data

data class MonsterDefinition(
    val name: String,
    val id: Int,
    val image: String,
    val attack: Int,
    val defense: Int,
    val hp: Int,
    val initiative: Int,
    val meatDrop: Int,
    val phylum: String,          // dude, beast, undead, etc.
    val isBoss: Boolean,
    val isGhost: Boolean,
    val isLucky: Boolean,
    val isScaling: Boolean,      // true if Scale: present
    val scale: Int,
    val cap: Int,
    val floor: Int,
    val drops: List<MonsterDrop>
)
