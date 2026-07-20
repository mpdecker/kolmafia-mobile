package net.sourceforge.kolmafia.data

data class MonsterDefinition(
    val name: String,
    val id: Int,
    val image: String,
    val attack: Int,
    /** Inner Atk: [expression] text when attack is expression-based; null for numeric Atk. */
    val attackExpression: String? = null,
    /** True when monsters.txt has an Atk: attribute (desktop attack != null). */
    val hasAttack: Boolean = false,
    val defense: Int,
    /** Inner Def: [expression] text when defense is expression-based; null for numeric Def. */
    val defenseExpression: String? = null,
    /** True when monsters.txt has a Def: attribute. */
    val hasDefense: Boolean = false,
    val hp: Int,
    /** Inner HP: [expression] text when HP is expression-based; null for numeric HP. */
    val hpExpression: String? = null,
    /** True when monsters.txt has an HP: attribute. */
    val hasHp: Boolean = false,
    val initiative: Int,
    /** False when monsters.txt has no Init: attribute (desktop initiative == null). */
    val hasInitiative: Boolean = true,
    /** Inner Init: [expression] text when initiative is expression-based; null for numeric Init. */
    val initiativeExpression: String? = null,
    val meatDrop: Int,
    val phylum: String,          // dude, beast, undead, etc.
    val isBoss: Boolean,
    val isGhost: Boolean,
    val isLucky: Boolean,
    val isScaling: Boolean,      // true if Scale: present
    val scale: Int,
    val cap: Int,
    val floor: Int,
    val article: String = "",
    val isCopyable: Boolean = true,
    val isWishable: Boolean = true,
    val poison: Int = Int.MAX_VALUE,
    val attackElement: String = "",
    val defenseElement: String = "",
    val drops: List<MonsterDrop>
) {
    companion object {
        const val DEFAULT_CAP = 10000
        const val DEFAULT_FLOOR = 10
    }
}
