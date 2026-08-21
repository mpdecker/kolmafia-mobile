package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] TakerSpace choice 1537 visit supplies sync.
 */
object TakerSpaceChoiceSync {

    const val CHOICE_ID = 1537
    const val TAKERSPACE_LETTER_OF_MARQUE_ID = 11687

    private val SUPPLIES = Regex(
        """<b>Current Supplies:</b><br>(\d+) stolen spices<br>(\d+) robbed rums<br>(\d+) absconded-with anchors?<br>(\d+) misappropriated mainmasts<br>(\d+) snatched silk<br>(\d+) gaffled gold<br>""",
        RegexOption.IGNORE_CASE,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        onSuppliesParsed: () -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt(
            CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF,
            TAKERSPACE_LETTER_OF_MARQUE_ID,
        )
        preferences.setBoolean("_takerSpaceSuppliesDelivered", true)
        SUPPLIES.find(html)?.let { match ->
            preferences.setInt("takerSpaceSpice", match.groupValues[1].toInt())
            preferences.setInt("takerSpaceRum", match.groupValues[2].toInt())
            preferences.setInt("takerSpaceAnchor", match.groupValues[3].toInt())
            preferences.setInt("takerSpaceMast", match.groupValues[4].toInt())
            preferences.setInt("takerSpaceSilk", match.groupValues[5].toInt())
            preferences.setInt("takerSpaceGold", match.groupValues[6].toInt())
            onSuppliesParsed()
        }
        return true
    }
}
