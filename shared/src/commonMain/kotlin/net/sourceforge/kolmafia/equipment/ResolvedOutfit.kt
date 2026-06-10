package net.sourceforge.kolmafia.equipment

/** Static (positive id) or custom (negative id) outfit resolved for wear/retrieve. */
data class ResolvedOutfit(
    val id: Int,
    val name: String,
    val pieces: List<String>,
) {
    val isCustom: Boolean get() = id < 0
    val isBirthdaySuit: Boolean get() = id == OutfitSpecial.BIRTHDAY_SUIT_ID
    val isPreviousOutfit: Boolean get() = id == OutfitSpecial.PREVIOUS_OUTFIT_ID
}

object OutfitSpecial {
    const val BIRTHDAY_SUIT_ID = 0
    const val PREVIOUS_OUTFIT_ID = -1

    /** KoL bug workaround: resolved negative id from equipment page, not literal "last". */
    var resolvedPreviousOutfitId: Int? = null

    fun isBirthdaySuitAlias(name: String): Boolean {
        val lower = name.trim().lowercase()
        return lower == "birthday suit" || lower == "nothing"
    }

    fun isPreviousOutfitAlias(name: String): Boolean =
        name.trim().equals("last", ignoreCase = true)
}
