package net.sourceforge.kolmafia.character

/**
 * Subset of the desktop AscensionPath enum covering paths with distinct behavioral
 * implications for automation: consumption restrictions, class-specific resources,
 * and special run conditions. The API returns the path name as a string; use
 * [fromApiString] to resolve it.
 */
enum class AscensionPath(
    val apiName: String,
    // Consumption restrictions
    val canEat: Boolean = true,
    val canDrink: Boolean = true,
    val canChew: Boolean = true,
    // Has non-standard class resources
    val hasPathResources: Boolean = false
) {
    NONE("None"),
    HARDCORE("Hardcore"),                           // not a real path; tracked separately

    // ── Consumption-restricted paths ─────────────────────────────────────────
    TEETOTALER("Teetotaler",            canDrink = false),
    BOOZETAFARIAN("Boozetafarian",      canEat = false),
    OXYGENARIAN("Oxygenarian",          canEat = false, canDrink = false),

    // ── Avatar / special class paths ─────────────────────────────────────────
    AVATAR_OF_BORIS("Avatar of Boris",              hasPathResources = true),
    ZOMBIE_SLAYER("Zombie Slayer",                  hasPathResources = true),
    AVATAR_OF_JARLSBERG("Avatar of Jarlsberg",      hasPathResources = true),
    AVATAR_OF_SNEAKY_PETE("Avatar of Sneaky Pete",  hasPathResources = true),
    ED("Actually Ed the Undying",                   hasPathResources = true),
    HEAVY_RAINS("Heavy Rains"),
    ACTUALLY_ED_THE_UNDYING("Actually Ed the Undying"),
    NUCLEAR_AUTUMN("Nuclear Autumn"),
    GELATINOUS_NOOB("Gelatinous Noob",              hasPathResources = true),
    YOU_ROBOT("You, Robot",             canDrink = false, canChew = false, hasPathResources = true),
    QUANTUM_TERRARIUM("Quantum Terrarium"),
    PLUMBER("Plumber",                  canDrink = false, hasPathResources = true),
    GREY_YOU("Grey You",                            hasPathResources = true),
    DARK_GYFFTE("Dark Gyffte"),
    TWO_CRAZY_RANDOM_SUMMER("Two Crazy Random Summer"),
    COMMUNITY_SERVICE("Community Service"),
    AVATAR_OF_WEST_OF_LOATHING("Avatar of West of Loathing",  hasPathResources = true),
    THE_SOURCE("The Source"),
    NUCLEAR("Nuclear Autumn"),
    LEGACY_OF_LOATHING("Legacy of Loathing"),
    PATH_OF_THE_PLUMBER("A Pocket Guide to Loathing"),   // alias
    WILDFIRE("Wildfire",                            hasPathResources = true),
    SMALL("Small"),
    SHADOWS_OVER_LOATHING("Shadows Over Loathing", hasPathResources = true),
    VAMPYRE("Vampyre",                  canEat = false),
    WEREPROFESSOR("WereProfessor"),
    ELEVEN_THINGS("11 Things I Hate About U"),
    AVANT_GUARD("Avant Guard"),
    UNDER_THE_SEA("Under the Sea"),
    Z_IS_FOR_ZOOTOMIST("Z Is for Zootomist"),
    HAT_TRICK("Hat Trick"),
    MEAT("Adventurer Meats World",      canDrink = false, canChew = false),
    THRIFTY("Thrifty"),
    LOW_KEY("Low Key"),
    KINGDOM_OF_EXPLOATHING("Kingdom of Exploathing"),
    SURPRISING_FIST("Way of the Surprising Fist"),
    STANDARD("Standard"),

    UNKNOWN("Unknown");

    val isAvatar: Boolean
        get() = this in setOf(
            AVATAR_OF_BORIS, AVATAR_OF_JARLSBERG, AVATAR_OF_SNEAKY_PETE,
            AVATAR_OF_WEST_OF_LOATHING, SHADOWS_OVER_LOATHING
        )

    companion object {
        private val byApiName: Map<String, AscensionPath> =
            entries.associateBy { it.apiName.lowercase() }

        fun fromApiString(s: String): AscensionPath =
            byApiName[s.lowercase().trim()] ?: UNKNOWN
    }
}
