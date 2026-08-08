package net.sourceforge.kolmafia.data

/** Desktop HellKitchenRequest static menu (cafeid=3). */
object HellKitchenDatabase {

    const val CAFE_ID = "3"

    private val entries = listOf(
        CafeMenuEntry("Jumbo Dr. Lucifer", CAFE_ID, 571, 150, ConsumableType.FOOD),
        CafeMenuEntry("Brimstone Chicken Sandwich", CAFE_ID, 570, 300, ConsumableType.FOOD),
        CafeMenuEntry("Lord of the Flies-sized fries", CAFE_ID, 569, 300, ConsumableType.FOOD),
        CafeMenuEntry("Double Bacon Beelzeburger", CAFE_ID, 568, 300, ConsumableType.FOOD),
        CafeMenuEntry("Imp Ale", CAFE_ID, 470, 75, ConsumableType.DRINK),
    )

    private val indexByName = entries.associateBy { it.name.lowercase() }

    fun isOnMenu(name: String): Boolean = indexByName.containsKey(name.lowercase())

    fun isFood(name: String): Boolean =
        indexByName[name.lowercase()]?.type == ConsumableType.FOOD

    fun resolve(name: String): CafeMenuEntry? = indexByName[name.lowercase()]
}
