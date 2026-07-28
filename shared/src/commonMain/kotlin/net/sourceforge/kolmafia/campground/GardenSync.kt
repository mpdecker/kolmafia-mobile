package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses garden crop type from campground HTML. Mirrors desktop [CampgroundRequest.parseGarden]
 * type detection (gif-family patterns only).
 */
object GardenSync {

    private val patternOrder = listOf(
        "pumpkinpatch_" to CropType.PUMPKIN,
        "pepperpatch_" to CropType.PEPPERMINT,
        "bonegarden" to CropType.SKELETON,
        "beergarden" to CropType.BEER,
        "wintergarden" to CropType.WINTER,
        "thanksgarden" to CropType.THANKSGARDEN,
        "grassgarden" to CropType.GRASS,
        "mushgarden.gif" to CropType.MUSHROOM,
        "/rockgarden/" to CropType.ROCK,
    )

    fun parseGardenType(html: String): CropType? {
        for ((pattern, type) in patternOrder) {
            if (html.contains(pattern, ignoreCase = true)) return type
        }
        return null
    }

    fun apply(character: KoLCharacter, html: String, preferences: Preferences? = null) {
        val type = parseGardenType(html)?.toString() ?: ""
        character.setCampground(gardenType = type)
        preferences?.setString("myGardenType", type)
    }
}
