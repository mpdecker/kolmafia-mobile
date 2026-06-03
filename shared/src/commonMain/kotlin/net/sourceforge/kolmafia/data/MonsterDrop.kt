package net.sourceforge.kolmafia.data

data class MonsterDrop(
    val itemName: String,
    val dropRate: Int,          // 0-100
    val prefix: Char?           // 'p','n','c','f','m','a' or null
)
