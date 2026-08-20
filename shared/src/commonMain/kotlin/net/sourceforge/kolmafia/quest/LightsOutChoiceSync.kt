package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter

/**
 * Desktop [ChoiceControl] Lights Out rooms 890–903.
 */
object LightsOutChoiceSync {

    const val ELIZABETH_PREF = "nextSpookyravenElizabethRoom"
    const val STEPHEN_PREF = "nextSpookyravenStephenRoom"
    const val COUNTER_LABEL = "Spookyraven Lights Out"
    const val LAST_TURN_PREF = "lastLightsOutTurn"

    private val ELIZABETH = mapOf(
        890 to ("BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!" to "The Haunted Laundry Room"),
        891 to ("DO YOU SEE THE STAIN UPON MY TOWEL?" to "The Haunted Bathroom"),
        892 to ("THE STAIN HAS BEEN LIFTED" to "The Haunted Kitchen"),
        893 to ("If You Give a Demon a Brownie" to "The Haunted Library"),
        894 to ("If You Give a Demon a Brownie" to "The Haunted Ballroom"),
        895 to ("The Flowerbed of Unearthly Delights" to "The Haunted Gallery"),
    )

    private val STEPHEN = mapOf(
        897 to ("restock his medical kit in the nursery" to "The Haunted Nursery"),
        898 to ("This afternoon we're burying Crumbles" to "The Haunted Conservatory"),
        899 to ("Crumbles isn't buried very deep" to "The Haunted Billiards Room"),
        900 to ("The wolf head has a particularly nasty expression on its face" to "The Haunted Wine Cellar"),
        901 to ("Crumbles II (Wolf)" to "The Haunted Boiler Room"),
        902 to ("CRUMBLES II" to "The Haunted Laboratory"),
    )

    fun applyVisit(choiceId: Int, preferences: Preferences?, turnsPlayed: Int): Boolean {
        if (choiceId !in 890..903) return false
        val prefs = preferences ?: return false
        TurnCounter.stopCounting(prefs, COUNTER_LABEL)
        prefs.setInt(LAST_TURN_PREF, turnsPlayed)
        return true
    }

    fun apply(choiceId: Int, html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        ELIZABETH[choiceId]?.let { (phrase, room) ->
            return advance(prefs, ELIZABETH_PREF, html, phrase, room)
        }
        STEPHEN[choiceId]?.let { (phrase, room) ->
            return advance(prefs, STEPHEN_PREF, html, phrase, room)
        }
        return false
    }

    private fun advance(
        preferences: Preferences,
        pref: String,
        html: String,
        phrase: String,
        nextRoom: String,
    ): Boolean {
        if (preferences.getString(pref, "") == "none") return false
        if (!html.contains(phrase)) return false
        preferences.setString(pref, nextRoom)
        return true
    }
}
