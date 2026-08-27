package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Wildfire NPCs Sprinkler Joe / Fracker Dan / Cropduster Dusty
 * choices 1452–1454.
 */
object WildfireNpcChoiceSync {

    const val SPRINKLER = 1452
    const val FRACKER = 1453
    const val CROPSTER = 1454

    val CHOICE_IDS = setOf(SPRINKLER, FRACKER, CROPSTER)

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || choiceId !in CHOICE_IDS) return false
        // postChoice0 thanks-text + postChoice1 raindrop paths
        return when (choiceId) {
            SPRINKLER -> {
                val helped = (decision == 1 && html.contains("raindrop.gif")) ||
                    html.contains("Thanks again for your help!")
                if (!helped) return false
                preferences.setBoolean("wildfireSprinkled", true)
                true
            }
            FRACKER -> {
                val helped = (decision == 1 && html.contains("raindrop.gif")) ||
                    html.contains("Thanks for the help!")
                if (!helped) return false
                preferences.setBoolean("wildfireFracked", true)
                true
            }
            CROPSTER -> {
                val helped = (decision == 1 && html.contains("raindrop.gif")) ||
                    html.contains("Thanks for helping out.")
                if (!helped) return false
                preferences.setBoolean("wildfireDusted", true)
                true
            }
            else -> false
        }
    }
}
