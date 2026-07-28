package net.sourceforge.kolmafia.campground

/** Garden crop type. Mirrors desktop [CampgroundRequest.CropType]. */
enum class CropType {
    PUMPKIN,
    PEPPERMINT,
    SKELETON,
    BEER,
    WINTER,
    THANKSGARDEN,
    GRASS,
    MUSHROOM,
    ROCK,
    ;

    override fun toString(): String = name.lowercase()
}
