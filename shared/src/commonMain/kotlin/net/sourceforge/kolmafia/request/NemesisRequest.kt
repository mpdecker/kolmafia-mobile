package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.NemesisManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Response and session-log handling for the Nemesis cave endpoints. */
object NemesisRequest {
    private val actionPattern = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
    private val itemPattern = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)

    fun action(url: String): String? = actionPattern.findAll(url).lastOrNull()?.groupValues?.get(1)

    fun registerRequest(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.substringAfterLast('/').startsWith("cave.php", ignoreCase = true)) return false
        val act = action(url) ?: return true
        val item = itemPattern.find(url)?.groupValues?.get(1)?.toIntOrNull()
        val message = when {
            act.equals("dodoor4", true) -> "Speaking password to door 4"
            act.equals("dodoor1", true) && item != null -> "Offering item #$item to door 1"
            act.equals("dodoor2", true) && item != null -> "Offering item #$item to door 2"
            act.equals("dodoor3", true) && item != null -> "Offering item #$item to door 3"
            act.equals("sanctum", true) -> null
            else -> return false
        }
        message?.let { sessionLogger?.appendRawLine(it) }
        return true
    }

    fun parseResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        questDatabase: QuestDatabase? = null,
        consumeItem: (itemId: Int, quantity: Int) -> Unit = { itemId, quantity ->
            inventory?.consumeItemLocally(itemId, quantity)
        },
    ): Boolean {
        if (url == null || !url.substringAfterLast('/').startsWith("cave.php", true)) return false
        val prefs = preferences ?: return false
        val signature = "$url:${html.hashCode()}"
        if (prefs.getString("_nemesisLastResponse", "") == signature) return false
        prefs.setString("_nemesisLastResponse", signature)
        val item = itemPattern.find(url)?.groupValues?.get(1)?.toIntOrNull()
        val consumed = item != null && (
            html.contains("stone slab slides", true) ||
                html.contains("stone slab grinds", true) ||
                html.contains("into the darkness", true)
            )
        if (consumed) consumeItem(item!!, 1)
        if (html.contains("your Nemesis is defeated", true) ||
            html.contains("You have defeated your nemesis", true)
        ) {
            questDatabase?.setProgress(Quest.NEMESIS, QuestDatabase.FINISHED)
        }
        return consumed
    }

    fun parsePaperStrip(itemId: Int, html: String, preferences: Preferences?): Boolean =
        preferences?.let { NemesisManager.parsePaperStripDescription(itemId, html, it) } ?: false
}
