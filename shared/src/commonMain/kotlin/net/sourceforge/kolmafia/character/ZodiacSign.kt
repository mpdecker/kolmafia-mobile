package net.sourceforge.kolmafia.character

enum class ZodiacStatType {
    MUSCLE,
    MYSTICALITY,
    MOXIE,
    BAD_MOON,
}

enum class ZodiacSign(
    val signName: String,
    private val statType: ZodiacStatType,
) {
    MONGOOSE("Mongoose", ZodiacStatType.MUSCLE),
    WALLABY("Wallaby", ZodiacStatType.MYSTICALITY),
    VOLE("Vole", ZodiacStatType.MOXIE),
    PLATYPUS("Platypus", ZodiacStatType.MUSCLE),
    OPOSSUM("Opossum", ZodiacStatType.MYSTICALITY),
    MARMOT("Marmot", ZodiacStatType.MOXIE),
    WOMBAT("Wombat", ZodiacStatType.MUSCLE),
    BLENDER("Blender", ZodiacStatType.MYSTICALITY),
    PACKRAT("Packrat", ZodiacStatType.MOXIE),
    BAD_MOON("Bad Moon", ZodiacStatType.BAD_MOON),
    ;

    val isMuscle: Boolean get() = statType == ZodiacStatType.MUSCLE
    val isMysticality: Boolean get() = statType == ZodiacStatType.MYSTICALITY
    val isMoxie: Boolean get() = statType == ZodiacStatType.MOXIE
    val isBadMoon: Boolean get() = statType == ZodiacStatType.BAD_MOON

    companion object {
        fun find(name: String): ZodiacSign? =
            entries.firstOrNull { it.signName.equals(name, ignoreCase = true) }
    }
}
