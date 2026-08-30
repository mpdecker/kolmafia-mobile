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
    val attributes: Set<String>,     // "sentient", "organic", "haswings", etc.
) {
    /** Column six in familiars.txt is the familiar's default equipment item. */
    val familiarItem: String
        get() = hatchlingItem

    private fun hasType(type: String) = type in types

    private fun hasAnyType(vararg typeCodes: String) = typeCodes.any { it in types }

    fun isCombatType(): Boolean = hasAnyType(
        "combat0", "combat1", "block", "delevel0", "delevel1", "hp0", "mp0", "other0",
    )

    fun isCombat0Type(): Boolean = hasType("combat0")
    fun isCombat1Type(): Boolean = hasType("combat1")
    fun isBlockType(): Boolean = hasType("block")
    fun isDelevelType(): Boolean = hasAnyType("delevel0", "delevel1")
    fun isHp0Type(): Boolean = hasType("hp0")
    fun isMp0Type(): Boolean = hasType("mp0")
    fun isOther0Type(): Boolean = hasType("other0")
    fun isHp1Type(): Boolean = hasType("hp1")
    fun isMp1Type(): Boolean = hasType("mp1")
    fun isOther1Type(): Boolean = hasType("other1")
    fun isPassiveType(): Boolean = hasType("passive")
    fun isUnderwaterType(): Boolean = hasType("underwater")
    fun isVariableType(): Boolean = hasType("variable")

    fun combinedAttributes(): String =
        attributes.joinToString("; ")

    val isPhysicalAttacker get() = isCombat0Type()
    val isElementalAttacker get() = isCombat1Type()
    val isMeatDropper get() = hasType("meat0")
    val isStatGainer get() = hasType("stat0") || hasType("stat1")
    val isItemDropper get() = hasAnyType("item0", "item1", "item2", "item3")
    val isUndead get() = "undead" in attributes
    val isPokefamType get() = hasType("pokefam")
}
