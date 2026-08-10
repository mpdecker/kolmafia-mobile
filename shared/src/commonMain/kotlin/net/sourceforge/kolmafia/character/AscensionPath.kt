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
    val hasPathResources: Boolean = false,
    // ASH $path[field] metadata (desktop Path enum)
    val pathId: Int = 0,
    val pathImage: String = "",
    val pointsPreference: String? = null,
    val avatarPath: Boolean = false,
    val allowsFamiliars: Boolean = true,
    val stomachCapacity: Int = 15,
    val liverCapacity: Int = 14,
    val spleenCapacity: Int = 15,
) {
    NONE("None"),
    HARDCORE("Hardcore"),                           // not a real path; tracked separately

    // ── Consumption-restricted paths ─────────────────────────────────────────
    TEETOTALER("Teetotaler", canDrink = false, pathId = 2, pathImage = "bowl.gif"),
    BOOZETAFARIAN("Boozetafarian", canEat = false, pathId = 1, pathImage = "martini.gif"),
    OXYGENARIAN("Oxygenarian", canEat = false, canDrink = false, pathId = 3, pathImage = "smalloxy.gif"),
    BEES_HATE_YOU("Bees Hate You", pathId = 4, pathImage = "beeicon"),

    // ── Avatar / special class paths ─────────────────────────────────────────
    AVATAR_OF_BORIS(
        "Avatar of Boris",
        hasPathResources = true,
        pathId = 8,
        pathImage = "trusty.gif",
        pointsPreference = "borisPoints",
        avatarPath = true,
        allowsFamiliars = false,
    ),
    ZOMBIE_SLAYER(
        "Zombie Slayer",
        hasPathResources = true,
        pathId = 10,
        pathImage = "tombstone.gif",
        pointsPreference = "zombiePoints",
        avatarPath = true,
    ),
    AVATAR_OF_JARLSBERG(
        "Avatar of Jarlsberg",
        hasPathResources = true,
        pathId = 12,
        pathImage = "jarlhat.gif",
        pointsPreference = "jarlsbergPoints",
        avatarPath = true,
        allowsFamiliars = false,
    ),
    AVATAR_OF_SNEAKY_PETE(
        "Avatar of Sneaky Pete",
        hasPathResources = true,
        pathId = 17,
        pathImage = "bigglasses.gif",
        pointsPreference = "sneakyPetePoints",
        avatarPath = true,
        allowsFamiliars = false,
    ),
    ED(
        "Actually Ed the Undying",
        hasPathResources = true,
        pathId = 23,
        pathImage = "scarab.gif",
        pointsPreference = "edPoints",
        avatarPath = true,
        allowsFamiliars = false,
    ),
    HEAVY_RAINS("Heavy Rains", pathId = 19, pathImage = "familiar31.gif"),
    ACTUALLY_ED_THE_UNDYING(
        "Actually Ed the Undying",
        hasPathResources = true,
        pathId = 23,
        pathImage = "scarab.gif",
        pointsPreference = "edPoints",
        avatarPath = true,
        allowsFamiliars = false,
    ),
    NUCLEAR_AUTUMN(
        "Nuclear Autumn",
        pathId = 28,
        pathImage = "radiation.gif",
        pointsPreference = "nuclearAutumnPoints",
        stomachCapacity = 3,
        liverCapacity = 2,
        spleenCapacity = 3,
    ),
    GELATINOUS_NOOB(
        "Gelatinous Noob",
        hasPathResources = true,
        pathId = 29,
        pathImage = "gcube.gif",
        pointsPreference = "noobPoints",
        avatarPath = true,
    ),
    LICENSE_TO_ADVENTURE(
        "License to Adventure",
        pathId = 30,
        pathImage = "briefcase.gif",
        pointsPreference = "bondPoints",
        liverCapacity = 2,
    ),
    YOU_ROBOT(
        "You, Robot",
        canDrink = false,
        canChew = false,
        hasPathResources = true,
        pathId = 41,
        pathImage = "robobattery.gif",
        pointsPreference = "youRobotPoints",
        stomachCapacity = 0,
        liverCapacity = 0,
        spleenCapacity = 0,
    ),
    QUANTUM_TERRARIUM(
        "Quantum Terrarium",
        pathId = 42,
        pathImage = "quantum.gif",
        pointsPreference = "quantumPoints",
    ),
    PLUMBER("Plumber", canDrink = false, hasPathResources = true),
    GREY_YOU(
        "Grey You",
        hasPathResources = true,
        pathId = 44,
        pathImage = "greygooring.gif",
        pointsPreference = "greyYouPoints",
        avatarPath = true,
    ),
    DARK_GYFFTE(
        "Dark Gyffte",
        pathId = 35,
        pathImage = "darkgift.gif",
        pointsPreference = "darkGyfftePoints",
        avatarPath = true,
        allowsFamiliars = false,
    ),
    TWO_CRAZY_RANDOM_SUMMER(
        "Two Crazy Random Summer",
        pathId = 36,
        pathImage = "twocrazydice.gif",
        pointsPreference = "twoCRSPoints",
    ),
    KOLHS("KOLHS", pathId = 15, pathImage = "kolhsicon"),
    COMMUNITY_SERVICE("Community Service", pathId = 25, pathImage = "csplaquesmall.gif"),
    AVATAR_OF_WEST_OF_LOATHING(
        "Avatar of West of Loathing",
        hasPathResources = true,
        pathId = 26,
        pathImage = "badge.gif",
    ),
    THE_SOURCE(
        "The Source",
        pathId = 27,
        pathImage = "ss_datasiphon.gif",
        pointsPreference = "sourcePoints",
    ),
    NUCLEAR("Nuclear Autumn"),
    LEGACY_OF_LOATHING(
        "Legacy of Loathing",
        pathId = 48,
        pathImage = "xx.gif",
        pointsPreference = "legacyPoints",
    ),
    PATH_OF_THE_PLUMBER(
        "A Pocket Guide to Loathing",
        pathId = 38,
        pathImage = "mario_mushroom1.gif",
        pointsPreference = "plumberPoints",
        avatarPath = true,
    ),
    WILDFIRE("Wildfire", hasPathResources = true, pathId = 43, pathImage = "fire.gif"),
    SMALL(
        "Small",
        pathId = 49,
        pathImage = "kiloskull.gif",
        stomachCapacity = 2,
        liverCapacity = 1,
    ),
    SHADOWS_OVER_LOATHING(
        "Shadows Over Loathing",
        hasPathResources = true,
        pathId = 47,
        pathImage = "aosol.gif",
    ),
    VAMPYRE("Vampyre", canEat = false),
    WEREPROFESSOR(
        "WereProfessor",
        pathId = 50,
        pathImage = "intrinsic_beast.gif",
        pointsPreference = "wereProfessorPoints",
        avatarPath = true,
        stomachCapacity = 5,
        liverCapacity = 4,
    ),
    ELEVEN_THINGS("11 Things I Hate About U", pathId = 51, pathImage = "ihatesu.gif"),
    AVANT_GUARD(
        "Avant Guard",
        pathId = 52,
        pathImage = "radshield.gif",
        pointsPreference = "avantGuardPoints",
    ),
    UNDER_THE_SEA(
        "Under the Sea",
        pathId = 55,
        pathImage = "fishy5.gif",
        pointsPreference = "seaPoints",
    ),
    Z_IS_FOR_ZOOTOMIST(
        "Z Is for Zootomist",
        pathId = 53,
        pathImage = "zootomist.gif",
        pointsPreference = "zootomistPoints",
        avatarPath = true,
    ),
    POKEFAM(
        "Pocket Familiars",
        pathId = 32,
        pathImage = "spiritorb.gif",
        pointsPreference = "pokefamPoints",
    ),
    GLOVER(
        "G-Lover",
        pathId = 33,
        pathImage = "g-loveheart.gif",
        pointsPreference = "gloverPoints",
    ),
    DISGUISES_DELIMIT(
        "Disguises Delimit",
        pathId = 34,
        pathImage = "dd_icon",
    ),
    DINOSAURS(
        "Fall of the Dinosaurs",
        pathId = 46,
        pathImage = "dinostuffy",
    ),
    HAT_TRICK("Hat Trick", pathId = 54, pathImage = "hat_bycocket.gif"),
    MEAT(
        "Adventurer Meats World",
        canDrink = false,
        canChew = false,
        pathId = 56,
        pathImage = "meat.gif",
        pointsPreference = "adventurerMeatsWorldPoints",
        avatarPath = true,
        stomachCapacity = 0,
        liverCapacity = 0,
        spleenCapacity = 0,
    ),
    THRIFTY("Thrifty", pathId = 57, pathImage = "0dollars.gif"),
    TRENDY("Trendy", pathId = 7, pathImage = "trendyicon.gif"),
    LOW_KEY("Low Key", pathId = 39, pathImage = "littlelock.gif"),
    KINGDOM_OF_EXPLOATHING("Kingdom of Exploathing", pathId = 37, pathImage = "puff.gif"),
    SURPRISING_FIST("Way of the Surprising Fist", pathId = 6, pathImage = "wosp_fist.gif"),
    STANDARD("Standard", pathId = 22, pathImage = "standardicon.gif"),
    SLOW_AND_STEADY("Slow and Steady", pathId = 18, pathImage = "sas"),

    UNKNOWN("Unknown");

    val isAvatar: Boolean
        get() = avatarPath || this in setOf(
            AVATAR_OF_BORIS,
            AVATAR_OF_JARLSBERG,
            AVATAR_OF_SNEAKY_PETE,
            AVATAR_OF_WEST_OF_LOATHING,
            SHADOWS_OVER_LOATHING,
        )

    fun canUseFamiliars(): Boolean = allowsFamiliars

    /** Desktop KoLCharacter.inSlowcore — Slow and Steady path disables average-adventure lookup. */
    fun inSlowcore(): Boolean = this == SLOW_AND_STEADY

    companion object {
        private val byApiName: Map<String, AscensionPath> =
            entries.associateBy { it.apiName.lowercase() }

        fun fromApiString(s: String): AscensionPath =
            byApiName[s.lowercase().trim()] ?: UNKNOWN
    }
}
