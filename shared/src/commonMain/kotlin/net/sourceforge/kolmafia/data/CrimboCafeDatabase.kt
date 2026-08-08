package net.sourceforge.kolmafia.data

/** Desktop CrimboCafeRequest static menu (cafeid=10). */
object CrimboCafeDatabase {

    const val CAFE_ID = "10"

    private val entries = listOf(
        CafeMenuEntry("Peppermint Nutrition Block", CAFE_ID, -104, 50, ConsumableType.FOOD),
        CafeMenuEntry("Gingerbread Nutrition Block", CAFE_ID, -105, 75, ConsumableType.FOOD),
        CafeMenuEntry("Cinnamon Nutrition Block", CAFE_ID, -106, 100, ConsumableType.FOOD),
        CafeMenuEntry("Fortified Eggnog Slurry", CAFE_ID, -107, 50, ConsumableType.DRINK),
        CafeMenuEntry("Hot Buttered Rum", CAFE_ID, -108, 75, ConsumableType.DRINK),
        CafeMenuEntry("Spicy Hot Chocolate", CAFE_ID, -109, 100, ConsumableType.DRINK),
    )

    private val indexByName = entries.associateBy { it.name.lowercase() }

    fun isOnMenu(name: String): Boolean = indexByName.containsKey(name.lowercase())

    fun resolve(name: String): CafeMenuEntry? = indexByName[name.lowercase()]
}
