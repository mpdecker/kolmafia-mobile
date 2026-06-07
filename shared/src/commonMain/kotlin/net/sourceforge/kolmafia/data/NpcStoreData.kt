package net.sourceforge.kolmafia.data

data class NpcStoreData(
    val storeKey: String,
    val storeName: String,
    val storeType: String,          // "NPC", "COIN", or "NPCCOIN"
    val items: List<NpcStoreItem> = emptyList()
) {
    val isNpc get() = storeType.contains("NPC")
    val isCoinmaster get() = storeType.contains("COIN")
}
