package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager

/**
 * Desktop [ChoiceAdventures.ChoiceCost] / [ChoiceAdventures.payCost] (Phases 1686–1700).
 * High-traffic spend rows only — not spoiler metadata.
 */
object ChoiceCost {

    enum class Kind { ITEM, MEAT, MP }

    data class Cost(
        val decision: Int,
        val kind: Kind,
        val amount: Int,
        val itemId: Int = 0,
    )

    private const val RUBBER_AXE = 292
    private const val VAMPIRE_HEART = 1518
    private const val SPANISH_FLY = 1633
    private const val HOBO_NICKEL = 3126
    private const val UNDERWORLD_ACORN = 4274
    private const val BAR_SKIN = 348

    private val TABLE: Map<Int, List<Cost>> = buildMap {
        fun add(choice: Int, vararg costs: Cost) {
            put(choice, costs.toList())
        }
        add(2, Cost(1, Kind.ITEM, -1, RUBBER_AXE))
        add(4, Cost(1, Kind.MEAT, -500), Cost(2, Kind.MEAT, -500))
        add(21, Cost(1, Kind.MEAT, -500))
        add(25, Cost(1, Kind.MEAT, -50), Cost(2, Kind.MEAT, -5000))
        add(47, Cost(1, Kind.ITEM, 1, VAMPIRE_HEART))
        add(72, Cost(1, Kind.ITEM, 5, SPANISH_FLY))
        add(127, Cost(2, Kind.ITEM, -3, ItemPool.PAPAYA))
        add(129, Cost(1, Kind.MEAT, -500))
        add(181, Cost(1, Kind.ITEM, 5, SPANISH_FLY))
        add(189, Cost(1, Kind.MEAT, -977))
        add(191, Cost(2, Kind.ITEM, -1, ItemPool.VALUABLE_TRINKET))
        add(230, Cost(1, Kind.ITEM, -30, HOBO_NICKEL))
        add(247, Cost(1, Kind.ITEM, -10, HOBO_NICKEL))
        add(
            250,
            Cost(1, Kind.ITEM, -250, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -150, HOBO_NICKEL),
            Cost(3, Kind.ITEM, -200, HOBO_NICKEL),
        )
        add(
            251,
            Cost(1, Kind.ITEM, -200, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -150, HOBO_NICKEL),
            Cost(3, Kind.ITEM, -250, HOBO_NICKEL),
        )
        add(
            252,
            Cost(1, Kind.ITEM, -250, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -200, HOBO_NICKEL),
            Cost(3, Kind.ITEM, -150, HOBO_NICKEL),
        )
        add(
            255,
            Cost(1, Kind.ITEM, -10, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -10, HOBO_NICKEL),
            Cost(3, Kind.ITEM, -10, HOBO_NICKEL),
        )
        add(258, Cost(1, Kind.ITEM, -99, HOBO_NICKEL))
        add(261, Cost(1, Kind.ITEM, -1000, HOBO_NICKEL))
        add(
            264,
            Cost(1, Kind.ITEM, -5, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -5, HOBO_NICKEL),
        )
        add(
            267,
            Cost(1, Kind.ITEM, -5, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -5, HOBO_NICKEL),
        )
        add(
            268,
            Cost(1, Kind.ITEM, -5, HOBO_NICKEL),
            Cost(2, Kind.ITEM, -5, HOBO_NICKEL),
        )
        add(275, Cost(1, Kind.ITEM, -10, HOBO_NICKEL))
        add(291, Cost(1, Kind.ITEM, -5, HOBO_NICKEL))
        add(292, Cost(1, Kind.ITEM, -5, HOBO_NICKEL))
        add(293, Cost(1, Kind.ITEM, -5, HOBO_NICKEL))
        add(294, Cost(1, Kind.ITEM, -5, HOBO_NICKEL))
        add(295, Cost(1, Kind.ITEM, -5, HOBO_NICKEL))
        add(304, Cost(1, Kind.MP, -200))
        add(305, Cost(1, Kind.ITEM, -1, ItemPool.MERKIN_PRESSUREGLOBE))
        add(
            310,
            Cost(1, Kind.ITEM, -10, ItemPool.DULL_FISH_SCALE),
            Cost(2, Kind.ITEM, -10, ItemPool.ROUGH_FISH_SCALE),
            Cost(4, Kind.ITEM, 10, ItemPool.DULL_FISH_SCALE),
            Cost(5, Kind.ITEM, 10, ItemPool.ROUGH_FISH_SCALE),
        )
        add(438, Cost(1, Kind.ITEM, -1, UNDERWORLD_ACORN))
        add(
            504,
            Cost(1, Kind.ITEM, -1, BAR_SKIN),
            Cost(2, Kind.ITEM, 1, BAR_SKIN),
            Cost(3, Kind.MEAT, -100),
        )
        add(507, Cost(1, Kind.ITEM, -1, ItemPool.TREE_HOLED_COIN))
        add(519, Cost(1, Kind.ITEM, -50, HOBO_NICKEL))
        add(873, Cost(1, Kind.MEAT, -500))
        add(1415, Cost(1, Kind.MEAT, -10000))
    }

    fun getCost(choice: Int, decision: Int): Cost? =
        TABLE[choice]?.firstOrNull { it.decision == decision }

    /**
     * Apply spend for [choice]/[decision]. Positive item amounts are gains and are skipped
     * (ResultProcessor handles acquires).
     */
    fun payCost(
        choice: Int,
        decision: Int,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ): Boolean {
        val cost = getCost(choice, decision) ?: return false
        if (cost.amount == 0) return false
        return when (cost.kind) {
            Kind.ITEM -> {
                if (cost.amount >= 0) return false
                val need = -cost.amount
                val have = inventory?.state?.value?.items?.get(cost.itemId)?.quantity ?: 0
                if (have < need) return false
                inventory?.consumeItemLocally(cost.itemId, need)
                true
            }
            Kind.MEAT -> {
                if (cost.amount >= 0) return false
                val need = -cost.amount
                val purse = character?.state?.value?.meat ?: 0
                if (purse < need) return false
                character?.updateMeat(purse - need)
                true
            }
            Kind.MP -> {
                if (cost.amount >= 0) return false
                val need = -cost.amount
                val state = character?.state?.value ?: return false
                if (state.currentMp < need) return false
                character.updateHpMp(
                    state.currentHp,
                    state.maxHp,
                    state.currentMp - need,
                    state.maxMp,
                )
                true
            }
        }
    }
}
