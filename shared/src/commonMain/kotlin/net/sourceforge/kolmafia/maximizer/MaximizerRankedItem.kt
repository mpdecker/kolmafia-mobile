package net.sourceforge.kolmafia.maximizer

data class MaximizerRankedItem(
    val itemId: Int,
    val name: String,
    val score: Double,
    val checked: MaximizerCheckedItem,
    var automatic: Boolean = false,
    var required: Boolean = false,
    /** Desktop CheckedItem.conditional — conditional-modifier equipment. */
    var conditional: Boolean = false,
    /** Desktop CheckedItem.single — at most one copy useful. */
    var single: Boolean = false,
) {
    /** Total acquisition count across all desktop CheckedItem channels. */
    val accessibleCount: Int get() = checked.totalCount()
}
