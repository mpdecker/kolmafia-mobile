package net.sourceforge.kolmafia.data

/**
 * Parsed adventures.txt zone row with formSource / adventureId
 * (desktop AdventureDatabase Adventure record — Phases 2586–2600).
 */
data class AdventureZone(
    val zoneName: String,
    val urlParams: String,
    val locationName: String,
    val environment: String,
    val diffLevel: String,
    val statRequirement: Int,
    val goals: List<String>,
    val isOverdrunk: Boolean,
    val noWander: Boolean,
    val forceNoncombat: Int = 0,
    val waterLevel: Int = 0,
) {
    /** Raw key before `.php` (adventure / place / casino / …). */
    val urlKey: String
        get() = urlParams.substringBefore('=').ifBlank { "adventure" }

    /** Value after `=` in adventures.txt col2. */
    val adventureId: String
        get() = urlParams.substringAfter('=', "").ifBlank { urlParams }

    /** Desktop formSource: `adventure.php`, `place.php`, `casino.php`, … */
    val formSource: String
        get() = if (urlKey.contains('.')) urlKey else "$urlKey.php"

    val snarfblat: String?
        get() = if (urlKey == "adventure") adventureId.takeIf { it.isNotBlank() } else null

    val isClanArea get() = urlParams.startsWith("clan_")
    val isCasino get() = urlKey == "casino"
    val isPlace get() = urlKey == "place"
    val isShadowRift get() = urlKey == "place" && adventureId == "shadow_rift"

    fun toLocation(): net.sourceforge.kolmafia.adventure.AdventureLocation =
        net.sourceforge.kolmafia.adventure.AdventureLocation(
            id = snarfblat ?: adventureId,
            name = locationName,
            zone = zoneName,
            formSource = formSource,
            adventureId = adventureId,
        )
}
