package net.sourceforge.kolmafia.data

/** Desktop ChezSnooteeRequest fixed menu (cafeid=1). */
object ChezSnooteeDatabase {

    const val CAFE_ID = "1"

    private val entries = listOf(
        CafeMenuEntry("Peche a la Frog", CAFE_ID, -1, 50, ConsumableType.FOOD),
        CafeMenuEntry("As Jus Gezund Heit", CAFE_ID, -2, 75, ConsumableType.FOOD),
        CafeMenuEntry("Bouillabaise Coucher Avec Moi", CAFE_ID, -3, 100, ConsumableType.FOOD),
    )

    private val indexByName = entries.associateBy { it.name.lowercase() }

    fun isOnMenu(name: String): Boolean = indexByName.containsKey(name.lowercase())

    fun resolve(name: String): CafeMenuEntry? = indexByName[name.lowercase()]

    fun find(query: String): CafeMenuEntry? = CafeMenuLookup.find(query, entries, ::resolve)
}
