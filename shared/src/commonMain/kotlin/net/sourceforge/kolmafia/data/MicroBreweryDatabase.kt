package net.sourceforge.kolmafia.data

/** Desktop MicroBreweryRequest fixed menu cases 0–2 (cafeid=2). */
object MicroBreweryDatabase {

    const val CAFE_ID = "2"

    private val entries = listOf(
        CafeMenuEntry("Petite Porter", CAFE_ID, -1, 50, ConsumableType.DRINK),
        CafeMenuEntry("Scrawny Stout", CAFE_ID, -2, 75, ConsumableType.DRINK),
        CafeMenuEntry("Infinitesimal IPA", CAFE_ID, -3, 100, ConsumableType.DRINK),
    )

    private val indexByName = entries.associateBy { it.name.lowercase() }

    fun isOnMenu(name: String): Boolean = indexByName.containsKey(name.lowercase())

    fun resolve(name: String): CafeMenuEntry? = indexByName[name.lowercase()]

    fun find(query: String): CafeMenuEntry? = CafeMenuLookup.find(query, entries, ::resolve)
}
