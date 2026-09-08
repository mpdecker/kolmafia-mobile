package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventures
import net.sourceforge.kolmafia.adventure.choice.ChoiceOption
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * Desktop [ChoiceControl] / [MonorailManager] On a Downtown Train choice 1308 —
 * visitChoice muffin order state + postChoice0 tin consumption + dynamic spoilers.
 */
object MonorailChoiceSync {

    const val CHOICE_ID = 1308
    const val EARTHENWARE_MUFFIN_TIN = 9596
    const val SHOVELFUL_OF_EARTH = 9539
    const val HUNK_OF_GRANITE = 9540

    private val MUFFIN_TYPE_PATTERN =
        Regex("""Looks like your order for a (.*? muffin) is not yet ready""")

    private val CHOICE_FORM_PATTERN =
        Regex("""name=choiceform\d+(.*?)</form>""", RegexOption.DOT_MATCHES_ALL)
    private val OPTION_PATTERN = Regex("""name=option value=(\d+)""")
    private val BUTTON_TEXT_PATTERN =
        Regex("""type=['"]?submit['"]? value=['"](.*?)['"]""", RegexOption.DOT_MATCHES_ALL)

    /** Desktop [MonorailManager.lyleSpoilers] — button text → (spoiler label, item name). */
    private val LYLE_SPOILERS: Map<String, Pair<String, String?>> = mapOf(
        "Exchange 10 shovelfuls of dirt and 10 hunks of granite for an earthenware muffin tin!" to
            ("" to "earthenware muffin tin"),
        "Order a blueberry muffin" to ("" to "blueberry muffin"),
        "Order a bran muffin" to ("" to "bran muffin"),
        "Order a chocolate chip muffin" to ("" to "chocolate chip muffin"),
        "Back to the Platform!" to ("" to null),
    )

    /**
     * Desktop [MonorailManager.choiceSpoilers] — options are dynamically numbered;
     * parse form HTML and map button text to muffin spoilers.
     */
    fun choiceSpoilers(
        choice: Int,
        html: String = ChoiceCombatAshState.lastChoiceResponseText,
    ): ChoiceAdventures.Spoilers? {
        if (choice != CHOICE_ID || html.isBlank()) return null
        val options = mutableListOf<ChoiceOption>()
        for (formMatch in CHOICE_FORM_PATTERN.findAll(html)) {
            val section = formMatch.groupValues[1]
            val choiceNumber = OPTION_PATTERN.find(section)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: continue
            val buttonText = BUTTON_TEXT_PATTERN.find(section)?.groupValues?.getOrNull(1) ?: continue
            val spoiler = LYLE_SPOILERS[buttonText] ?: continue
            val itemName = spoiler.second
            // Prefer item name when desktop spoiler label is empty (ASH display).
            options += ChoiceOption(
                name = spoiler.first.ifBlank { itemName.orEmpty() },
                option = choiceNumber,
                itemNames = listOfNotNull(itemName),
            )
        }
        if (options.isEmpty()) return null
        return ChoiceAdventures.Spoilers(choice, "On a Downtown Train", options)
    }

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
