package net.sourceforge.kolmafia.character

enum class ZodiacStatType {
    MUSCLE,
    MYSTICALITY,
    MOXIE,
    BAD_MOON,
}

enum class ZodiacSign(
    val signName: String,
    val id: Int,
    private val statType: ZodiacStatType,
) {
    MONGOOSE("Mongoose", 1, ZodiacStatType.MUSCLE),
    WALLABY("Wallaby", 2, ZodiacStatType.MYSTICALITY),
    VOLE("Vole", 3, ZodiacStatType.MOXIE),
    PLATYPUS("Platypus", 4, ZodiacStatType.MUSCLE),
    OPOSSUM("Opossum", 5, ZodiacStatType.MYSTICALITY),
    MARMOT("Marmot", 6, ZodiacStatType.MOXIE),
    WOMBAT("Wombat", 7, ZodiacStatType.MUSCLE),
    BLENDER("Blender", 8, ZodiacStatType.MYSTICALITY),
    PACKRAT("Packrat", 9, ZodiacStatType.MOXIE),
    BAD_MOON("Bad Moon", 10, ZodiacStatType.BAD_MOON),
    ;

    val isMuscle: Boolean get() = statType == ZodiacStatType.MUSCLE
    val isMysticality: Boolean get() = statType == ZodiacStatType.MYSTICALITY
    val isMoxie: Boolean get() = statType == ZodiacStatType.MOXIE
    val isBadMoon: Boolean get() = statType == ZodiacStatType.BAD_MOON

    companion object {
        fun find(name: String): ZodiacSign? =
            entries.firstOrNull { it.signName.equals(name, ignoreCase = true) }

        fun find(id: Int): ZodiacSign? =
            entries.firstOrNull { it.id == id }
    }
}
