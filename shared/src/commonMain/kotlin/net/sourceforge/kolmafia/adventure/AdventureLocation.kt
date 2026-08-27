package net.sourceforge.kolmafia.adventure

data class AdventureLocation(
    val id: String,
    val name: String,
    val zone: String,
    val formSource: String = "adventure.php",
    val adventureId: String = id,
) {
    val usesSnarfblat: Boolean
        get() = formSource == "adventure.php" ||
            (formSource == "place.php" && adventureId == ShadowRift.ADVENTURE_ID)
}
