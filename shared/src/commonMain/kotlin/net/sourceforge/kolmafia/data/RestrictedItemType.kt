package net.sourceforge.kolmafia.data

/** Desktop [RestrictedItemType]. */
enum class RestrictedItemType {
    ITEMS,
    CAMPGROUND,
    BOOKSHELF_BOOKS,
    SKILLS,
    FAMILIARS,
    CLAN_ITEMS,
    MISCELLANEOUS;

    companion object {
        fun fromString(type: String): RestrictedItemType? = when (type.trim()) {
            "Items" -> ITEMS
            "Campground" -> CAMPGROUND
            "Bookshelf", "Bookshelf Books" -> BOOKSHELF_BOOKS
            "Skills" -> SKILLS
            "Familiars" -> FAMILIARS
            "Clan Item", "Clan Items" -> CLAN_ITEMS
            "Miscellaneous" -> MISCELLANEOUS
            else -> null
        }
    }
}
