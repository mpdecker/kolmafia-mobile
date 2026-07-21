package net.sourceforge.kolmafia.character

/** Desktop [KoLCharacter.getBeeosity] — count b/B in equipment item names. */
object Beeosity {

    fun itemBeeosity(name: String?): Int =
        name?.count { it == 'b' || it == 'B' } ?: 0

    fun hasBeeosity(name: String?): Boolean =
        name?.any { it == 'b' || it == 'B' } == true

    fun equipmentBeeosity(equipment: Map<EquipmentSlot, String>): Int =
        equipment.values.sumOf { itemBeeosity(it) }
}
