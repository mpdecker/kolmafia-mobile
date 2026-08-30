package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SessionLogger

/** Non-UI Tavern endpoint parser: keys, goofballs, and RAT quest state. */
object TavernRequest {
    private val goofballCost = Regex("""Buy some goofballs\s*\((\d+),000 Meat\)""", RegexOption.IGNORE_CASE)
    private val squarePattern = Regex("""whichspot=([\d,]+)""", RegexOption.IGNORE_CASE)

    fun registerRequest(url: String, logger: SessionLogger?): Boolean {
        if (!url.substringAfterLast('/').startsWith("tavern.php", true)) return false
        val action = url.substringAfter("action=", "").substringBefore('&')
        val message = when {
            action.equals("buygoofballs", true) -> "Buying goofballs"
            url.contains("place=barkeep", true) -> "Visiting Bart Ender"
            url.contains("place=susguy", true) -> "Visiting the tavern goofball seller"
            else -> null
        }
        message?.let { logger?.appendRawLine(it) }
        return true
    }

    fun parseResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        ascensionNumber: Int = 0,
        consumeItem: (itemId: Int, quantity: Int) -> Unit = { _, _ -> },
        spendMeat: (Long) -> Unit = {},
    ): Boolean {
        if (url == null || !url.substringAfterLast('/').startsWith("tavern.php", true)) return false
        val prefs = preferences ?: return false
        val signature = "$url:${html.hashCode()}"
        if (prefs.getString("_tavernLastResponse", "") == signature) return false
        prefs.setString("_tavernLastResponse", signature)
        var changed = false
        if (html.contains("have a few drinks on the house", true) ||
            html.contains("something that wasn't booze", true) ||
            html.contains("a round on the house", true)
        ) {
            questDatabase?.setProgress(Quest.RAT, QuestDatabase.FINISHED)
            changed = true
        } else if (url.contains("place=barkeep", true)) {
            questDatabase?.setQuestIfBetter(Quest.RAT, "step1")
            changed = true
        }
        if (url.contains("place=susguy", true) && !html.contains("Take some goofballs", true)) {
            prefs.setInt("lastGoofballBuy", ascensionNumber)
            changed = true
        }
        if (url.contains("action=buygoofballs", true)) {
            prefs.setInt("lastGoofballBuy", ascensionNumber)
            val cost = goofballCost.find(html)?.groupValues?.get(1)?.toLongOrNull()
            if (cost != null && html.contains("If you get caught", true)) {
                spendMeat(cost * 1000L)
            }
            changed = true
        }
        // The barkeep accepts the gloomy black mushroom as the cellar key.
        if (url.contains("place=barkeep", true) &&
            html.contains("takes your gloomy black mushroom", true)
        ) {
            consumeItem(879, 1)
            changed = true
        }
        return changed
    }

    fun cellarLocationString(url: String): String? {
        val square = squarePattern.find(url)?.groupValues?.get(1)
            ?.replace(",", "")?.toIntOrNull() ?: return null
        if (square !in 1..25) return null
        return "The Typical Tavern Cellar (row ${(square - 1) / 5 + 1}, col ${(square - 1) % 5 + 1})"
    }
}
