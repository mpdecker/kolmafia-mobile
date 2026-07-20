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
    /** Inner Scale: [expression] text; null for numeric Scale / ?. */
    val scaleExpression: String? = null,
    val cap: Int,
    /** Inner Cap: [expression] text; null for numeric Cap / ?. */
    val capExpression: String? = null,
    val floor: Int,
    /** Inner Floor: [expression] text; null for numeric Floor / ?. */
    val floorExpression: String? = null,
    /** Numeric Exp: when present; 0 if expression or absent. */
    val experience: Int = 0,
    /** Inner Exp: [expression] text; null for numeric Exp / absent. */
    val experienceExpression: String? = null,
    /** True when monsters.txt has an Exp: attribute. */
    val hasExperience: Boolean = false,
    /**
     * Numeric MLMult: when present; default 1 when absent.
     * Desktop [MonsterData.ML] = globalMl × evaluate(mlMult, 1).
     */
    val mlMult: Int = 1,
    /** Inner MLMult: [expression] text; null for numeric MLMult / absent. */
    val mlMultExpression: String? = null,
    /** True when monsters.txt has an MLMult: attribute. */
    val hasMlMult: Boolean = false,
    val article: String = "",
    val isCopyable: Boolean = true,
    val isWishable: Boolean = true,
    val poison: Int = Int.MAX_VALUE,
    val attackElement: String = "",
    val defenseElement: String = "",
    val physicalResistance: Int = 0,
    val physicalResistanceExpression: String? = null,
    val elementalResistance: Int = 0,
    val elementalResistanceExpression: String? = null,
    val hotResistance: Int = 0,
    val hotResistanceExpression: String? = null,
    val coldResistance: Int = 0,
    val coldResistanceExpression: String? = null,
    val stenchResistance: Int = 0,
    val stenchResistanceExpression: String? = null,
    val spookyResistance: Int = 0,
    val spookyResistanceExpression: String? = null,
    val sleazeResistance: Int = 0,
    val sleazeResistanceExpression: String? = null,
    val drops: List<MonsterDrop>
) {
    companion object {
        const val DEFAULT_CAP = 10000
        const val DEFAULT_FLOOR = 10
    }
}
