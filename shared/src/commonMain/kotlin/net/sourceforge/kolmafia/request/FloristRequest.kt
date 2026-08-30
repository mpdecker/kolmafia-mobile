package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.request.FloristRequest] — Florist Friar plant catalog
 * and choice 720 parse.
 */
object FloristRequest {
    enum class Florist(val id: Int, val plantName: String) {
        RABID_DOGWOOD(1, "Rabid Dogwood"),
        RUTABEGGAR(2, "Rutabeggar"),
        RADISH(3, "Rad-ish Radish"),
        ARTICHOKER(4, "Artichoker"),
        SMOKERA(5, "Smoke-ra"),
        SKUNK_CABBAGE(6, "Skunk Cabbage"),
        DEADLY_CINNAMON(7, "Deadly Cinnamon"),
        CELERY_STALKER(8, "Celery Stalker"),
        LETTUCE_SPRAY(9, "Lettuce Spray"),
        SELTZER_WATERCRESS(10, "Seltzer Watercress"),
        WAR_LILY(11, "War Lily"),
        STEALING_MAGNOLIA(12, "Stealing Magnolia"),
        CANNED_SPINACH(13, "Canned Spinach"),
        IMPATIENS(14, "Impatiens"),
        SPIDER_PLANT(15, "Spider Plant"),
        RED_FERN(16, "Red Fern"),
        BAMBOO(17, "BamBOO!"),
        ARCTIC_MOSS(18, "Arctic Moss"),
        ALOE_GUVNOR(19, "Aloe Guv'nor"),
        PITCHER_PLANT(20, "Pitcher Plant"),
        BLUSTERY_PUFFBALL(21, "Blustery Puffball"),
        HORN_OF_PLENTY(22, "Horn of Plenty"),
        WIZARD_WIG(23, "Wizard's Wig"),
        SHUFFLE_TRUFFLE(24, "Shuffle Truffle"),
        DIS_LICHEN(25, "Dis Lichen"),
        LOOSE_MORELS(26, "Loose Morels"),
        FOUL_TOADSTOOL(27, "Foul Toadstool"),
        CHILLTERELLE(28, "Chillterelle"),
        PORTLYBELLA(29, "Portlybella"),
        MAX_HEADSHROOM(30, "Max Headshroom"),
        SPANKTON(31, "Spankton"),
        KELPTOMANIAC(32, "Kelptomaniac"),
        CROOKWEED(33, "Crookweed"),
        ELECTRIC_EELGRASS(34, "Electric Eelgrass"),
        DUCKWEED(35, "Duckweed"),
        ORCA_ORCHID(36, "Orca Orchid"),
        SARGASSUM(37, "Sargassum"),
        SUBSEA_ROSE(38, "Sub-Sea Rose"),
        SNORI(39, "Snori"),
        UPSEA_DAISY(40, "Up Sea Daisy"),
        ;

        override fun toString(): String = plantName

        fun isTerritorial(): Boolean = id % 10 == 1 || id % 10 == 2 || id % 10 == 3

        companion object {
            fun getFlower(id: Int): Florist? =
                if (id == 0) null else entries.firstOrNull { it.id == id }

            fun getFlower(name: String?): Florist? =
                name?.let { query -> entries.firstOrNull { it.plantName.equals(query, ignoreCase = true) } }
        }
    }

    private val FLOWER_PATTERN = Regex(
        """<tr><td>([^>]*?)</td><td width.*?plant(\d+)\.gif.*?(?:plant(\d+)\.gif)?.*?(?:plant(\d+)\.gif)?""",
    )
    private val LOCATION_PATTERN = Regex("""Ah, <b>(.*?)</b>!""")

    val floristPlants = linkedMapOf<String, MutableList<Florist>>()

    fun reset() {
        floristPlants.clear()
    }

    fun haveFlorist(preferences: Preferences?): Boolean {
        if (preferences?.getBoolean("floristFriarChecked", false) != true) return false
        return preferences.getBoolean("floristFriarAvailable", false)
    }

    fun setFloristFriarAvailable(preferences: Preferences?, available: Boolean) {
        preferences?.setBoolean("floristFriarChecked", true)
        preferences?.setBoolean("floristFriarAvailable", available)
    }

    fun getPlants(location: String): List<Florist> =
        floristPlants[location]?.toList().orEmpty()

    fun parseResponse(urlString: String, responseText: String, preferences: Preferences?): Boolean {
        if (!urlString.contains("choice.php", ignoreCase = true) ||
            !urlString.contains("whichchoice=720", ignoreCase = true)
        ) {
            return false
        }
        if (responseText.contains("The Florist Friar's Cottage")) {
            setFloristFriarAvailable(preferences, true)
        }
        val location = LOCATION_PATTERN.find(responseText)?.groupValues?.getOrNull(1)
        when (option(urlString)) {
            1 -> {
                val plant = plant(urlString)
                if (plant == 0) return true
                if (responseText.contains("You need to dig up a space.") ||
                    responseText.contains("Invalid plant")
                ) {
                    return true
                }
                if (location != null) addPlant(location, plant, preferences)
            }
            2 -> if (location != null && responseText.contains("You dig up a plant.")) {
                digPlant(location, digIndex(urlString))
            }
            4 -> {
                floristPlants.clear()
                FLOWER_PATTERN.findAll(responseText).forEach { match ->
                    val loc = match.groupValues[1]
                    val plants = match.groupValues.drop(2).mapNotNull { it.toIntOrNull() }
                        .mapNotNull { Florist.getFlower(it) }
                        .toMutableList()
                    if (plants.isNotEmpty()) floristPlants[loc] = plants
                }
            }
        }
        return true
    }

    fun addPlant(location: String, plantId: Int, preferences: Preferences? = null) {
        val plant = Florist.getFlower(plantId) ?: return
        if (plant.isTerritorial()) clearTerritorial(location)
        val plants = floristPlants.getOrPut(location) { mutableListOf() }
        plants.add(plant)
        val used = preferences?.getString("_floristPlantsUsed", "").orEmpty()
        preferences?.setString(
            "_floristPlantsUsed",
            if (used.isBlank()) plant.plantName else "$used,${plant.plantName}",
        )
    }

    private fun clearTerritorial(location: String) {
        val plants = floristPlants[location] ?: return
        val idx = plants.indexOfFirst { it.isTerritorial() }
        if (idx >= 0) plants.removeAt(idx)
    }

    private fun digPlant(location: String, digIndex: Int) {
        val plants = floristPlants[location] ?: return
        if (digIndex !in plants.indices) return
        plants.removeAt(digIndex)
    }

    private fun option(url: String): Int =
        Regex("""(?:^|[?&])option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    private fun plant(url: String): Int =
        Regex("""(?:^|[?&])plant=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    private fun digIndex(url: String): Int =
        Regex("""(?:^|[?&])plnti=(\d)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: -1
}
