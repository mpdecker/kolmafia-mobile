package net.sourceforge.kolmafia.data

/** Desktop [SkillDatabase.Category] — skill class/path grouping by id range. */
enum class SkillCategory(val displayName: String) {
    UNKNOWN("unknown"),
    UNCATEGORIZED("uncategorized"),
    SEAL_CLUBBER("seal clubber"),
    TURTLE_TAMER("turtle tamer"),
    PASTAMANCER("pastamancer"),
    SAUCEROR("sauceror"),
    DISCO_BANDIT("disco bandit"),
    ACCORDION_THIEF("accordion thief"),
    CONDITIONAL("conditional"),
    MR_SKILLS("mr. skills"),
    NINE("9XXX"),
    TEN("10XXX"),
    AVATAR_OF_BORIS("avatar of Boris"),
    ZOMBIE_MASTER("zombie master"),
    THIRTEEN("13XXX"),
    AVATAR_OF_JARLSBERG("Avatar of Jarlsberg"),
    AVATAR_OF_SNEAKY_PETE("Avatar of Sneaky Pete"),
    HEAVY_RAINS("Heavy Rains"),
    ED("Ed the Undying"),
    COW_PUNCHER("Cow Puncher"),
    BEANSLINGER("Beanslinger"),
    SNAKE_OILER("Snake Oiler"),
    SOURCE("The Source"),
    NUCLEAR_AUTUMN("Nuclear Autumn"),
    GELATINOUS_NOOB("Gelatinous Noob"),
    VAMPYRE("Vampyre"),
    PLUMBER("Plumber"),
    TWENTY_SIX("26XXX"),
    GREY_YOU("Grey You"),
    PIG_SKINNER("Pig Skinner"),
    CHEESE_WIZARD("Cheese Wizard"),
    JAZZ_AGENT("Jazz Agent"),
    THIRTY_ONE("31XXX"),
    THIRTY_TWO("32XXX"),
    MEAT_GOLEM("Meat Golem"),
    GNOME_SKILLS("gnome trainer"),
    BAD_MOON("bad moon"),
    ;

    companion object {
        private val byIndex = entries.toTypedArray()

        fun bySkillId(skillId: Int): SkillCategory {
            val categoryId = skillId / 1000 + 1
            if (categoryId >= byIndex.size - 2) {
                return UNKNOWN
            }
            return when (skillId) {
                in MR_SKILL_IDS -> MR_SKILLS
                in GNOME_SKILL_IDS -> GNOME_SKILLS
                in BAD_MOON_SKILL_IDS -> BAD_MOON
                MUG_FOR_THE_AUDIENCE -> AVATAR_OF_SNEAKY_PETE
                else -> byIndex.getOrElse(categoryId) { UNKNOWN }
            }
        }

        private const val MUG_FOR_THE_AUDIENCE = 7201

        private val MR_SKILL_IDS = setOf(
            7208, // SMILE_OF_MR_A
            7213, 7214, 7215, 7216, 7217, 7218, 7219, 7220, 7221, 7222,
            7223, 7224, 7225, 7226, 7227, 7228, 7229, 7230,
        )

        private val GNOME_SKILL_IDS = setOf(10, 11, 12, 13, 14)

        private val BAD_MOON_SKILL_IDS = setOf(21, 22, 23, 24, 25, 26, 27)
    }
}
