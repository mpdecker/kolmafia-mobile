package net.sourceforge.kolmafia.character

enum class MainStat { MUSCLE, MYSTICALITY, MOXIE }

enum class CharacterClass(
    val id: Int,
    val displayName: String,
    val mainStat: MainStat,
    val primeStatName: String
) {
    SEAL_CLUBBER(1,    "Seal Clubber",    MainStat.MUSCLE,        "Muscle"),
    TURTLE_TAMER(2,    "Turtle Tamer",    MainStat.MUSCLE,        "Muscle"),
    PASTAMANCER(3,     "Pastamancer",     MainStat.MYSTICALITY,   "Mysticality"),
    SAUCEROR(4,        "Sauceror",        MainStat.MYSTICALITY,   "Mysticality"),
    DISCO_BANDIT(5,    "Disco Bandit",    MainStat.MOXIE,         "Moxie"),
    ACCORDION_THIEF(6, "Accordion Thief", MainStat.MOXIE,         "Moxie"),
    // Avatar / special paths — encountered as temporary classes
    ED(7,              "Ed the Undying",  MainStat.MUSCLE,        "Muscle"),
    COW_PUNCHER(8,     "Cow Puncher",     MainStat.MUSCLE,        "Muscle"),
    BEANSLINGER(9,     "Beanslinger",     MainStat.MYSTICALITY,   "Mysticality"),
    SNAKE_OILER(10,    "Snake Oiler",     MainStat.MOXIE,         "Moxie"),
    GELATINOUS_NOOB(11,"Gelatinous Noob", MainStat.MYSTICALITY,   "Mysticality"),
    UNKNOWN(0,         "Unknown",         MainStat.MUSCLE,        "Muscle");

    val isMuscleBased      get() = mainStat == MainStat.MUSCLE
    val isMysticality      get() = mainStat == MainStat.MYSTICALITY
    val isMoxieBased       get() = mainStat == MainStat.MOXIE
    val isStandardClass    get() = id in 1..6

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: Int): CharacterClass = byId[id] ?: UNKNOWN

        // Returns the display name for a given class ID without needing an enum lookup.
        fun nameForId(id: Int): String = fromId(id).displayName
    }
}
