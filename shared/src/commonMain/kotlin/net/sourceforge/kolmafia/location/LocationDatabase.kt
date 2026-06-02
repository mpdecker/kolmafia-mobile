package net.sourceforge.kolmafia.location

object LocationDatabase {

    val ALL_LOCATIONS: List<LocationData> = listOf(
        // Level 1–4: Starter zones
        LocationData("18",  "The Haunted Pantry",                    "The Nearby Plains",           1),
        LocationData("20",  "The Spooky Forest",                     "The Nearby Plains",           1),
        LocationData("23",  "The Degrassi Knoll Garage",             "The Nearby Plains",           2),
        LocationData("24",  "The Degrassi Knoll Gym",                "The Nearby Plains",           2),
        LocationData("28",  "Cobb's Knob Kitchen",                   "Cobb's Knob",                 3),
        LocationData("29",  "Cobb's Knob Harem",                     "Cobb's Knob",                 3),
        LocationData("26",  "Outskirts of Cobb's Knob",              "Cobb's Knob",                 3),
        LocationData("27",  "Cobb's Knob Barracks",                  "Cobb's Knob",                 4),
        LocationData("30",  "Cobb's Knob Treasury",                  "Cobb's Knob",                 4),
        LocationData("219", "Cobb's Knob Laboratory",                "Cobb's Knob",                 4),
        // Level 5–8: Mid-game
        LocationData("93",  "The Haunted Conservatory",              "Spookyraven Manor",           5),
        LocationData("101", "The Haunted Ballroom",                  "Spookyraven Manor",           5),
        LocationData("106", "The Haunted Library",                   "Spookyraven Manor",           6),
        LocationData("125", "The Haunted Wine Cellar",               "Spookyraven Manor",           7),
        LocationData("140", "A-Boo Peak",                            "The Big Mountains",           7),
        LocationData("141", "Twin Peak",                             "The Big Mountains",           7),
        LocationData("142", "The Lair of the Ninja Snowmen",         "The Big Mountains",           8),
        LocationData("128", "Whitey's Grove",                        "The Nearby Plains",           8),
        // Level 9–12: Late mid-game
        LocationData("131", "The Battlefield (Frat Uniform)",        "The Mysterious Island",       9),
        LocationData("132", "The Battlefield (Hippy Outfit)",        "The Mysterious Island",       9),
        LocationData("150", "The Valley of Rof L'm Fao",             "The Penultimate Fantasy",    10),
        LocationData("167", "The Dungeons of Doom",                  "The Nearby Plains",          11),
        LocationData("176", "The Slime Tube",                        "The Nearby Plains",          11),
        LocationData("180", "The Penultimate Fantasy Airship",       "The Penultimate Fantasy",    11),
        LocationData("184", "The Castle in the Clouds (Basement)",   "The Penultimate Fantasy",    12),
        LocationData("185", "The Castle in the Clouds (Ground)",     "The Penultimate Fantasy",    12),
        LocationData("186", "The Castle in the Clouds (Top)",        "The Penultimate Fantasy",    12),
        // Seasonal / special
        LocationData("191", "Fernswarthy's Tower (Second Floor)",    "The Nearby Plains",          10),
        LocationData("406", "Dreadsylvania",                         "Dreadsylvania",              10),
        LocationData("385", "The Briniest Deepests",                 "The Sea",                    12),
        // Clan
        LocationData("371", "The Haunted Storage Room",              "Dreadsylvania",              10),
        // Farm zones
        LocationData("100", "The Degrassi Knoll Restroom",           "The Nearby Plains",           2),
        LocationData("337", "Uncle Gator's Country Fun-Time Liquid Waste Sluice", "The Nearby Plains", 1),
        // Goblin King
        LocationData("31",  "The Throne Room",                       "Cobb's Knob",                 5)
    )

    fun search(query: String): List<LocationData> {
        if (query.isBlank()) return ALL_LOCATIONS
        val q = query.trim().lowercase()
        return ALL_LOCATIONS.filter {
            it.name.lowercase().contains(q) || it.zone.lowercase().contains(q)
        }
    }

    fun findBySnarfblat(snarfblat: String): LocationData? =
        ALL_LOCATIONS.find { it.snarfblat == snarfblat }
}
