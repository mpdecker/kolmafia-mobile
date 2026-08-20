package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] On a Downtown Train choice 1308 —
 * visitChoice muffin order state + postChoice0 tin consumption.
 */
object MonorailChoiceSync {

    const val CHOICE_ID = 1308
    const val EARTHENWARE_MUFFIN_TIN = 9596
    const val SHOVELFUL_OF_EARTH = 9539
    const val HUNK_OF_GRANITE = 9540

    private val MUFFIN_TYPE_PATTERN =
        Regex("""Looks like your order for a (.*? muffin) is not yet ready""")

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        val muffinMatch = MUFFIN_TYPE_PATTERN.find(html)
        when {
            muffinMatch != null -> {
                preferences.setString("muffinOnOrder", muffinMatch.groupValues[1])
                changed = true
            }
            html.contains("you placed your order a lifetime ago") ||
                html.contains("You spot your order from the other day") ||
                html.contains("Order a blueberry muffin") -> {
                preferences.setString("muffinOnOrder", "none")
                changed = true
            }
        }
        if (html.contains("Here's your muffin tin!")) {
            consumeItem(SHOVELFUL_OF_EARTH, 10)
            consumeItem(HUNK_OF_GRANITE, 10)
            changed = true
        }
        return changed
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        visitHtml: String? = null,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = applyVisit(choiceId, html, preferences, consumeItem)
        if (html.contains("muffin is not yet ready")) {
            preferences.setBoolean("_muffinOrderedToday", true)
            changed = true
            if (visitHtml?.contains("Order a blueberry muffin") == true) {
                consumeItem(EARTHENWARE_MUFFIN_TIN, 1)
            }
        }
        return changed
    }
}
