package net.sourceforge.kolmafia.data

data class SkillDefinition(
    val id: Int,
    val name: String,
    val image: String,
    val tags: Set<String>,         // "passive", "combat", "nc", "heal", etc.
    val mpCost: Int,
    val duration: Int,
    val isPermable: Boolean = true, // from "Permable: false" attribute
    val guildLevel: Int = 0, // from "Level: N" attribute in classskills.txt
    val isPassive: Boolean,
    val isCombat: Boolean,
    val isNonCombat: Boolean,
    val isSong: Boolean
)
