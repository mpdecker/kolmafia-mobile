package net.sourceforge.kolmafia.data

data class OutfitData(
    val id: Int,
    val name: String,
    val image: String,
    val equipment: List<String>,        // item names required to complete outfit
    val halloweenDrops: List<String>    // raw drop strings (may include quantity/chance)
) {
    val pieceCount get() = equipment.size
}
