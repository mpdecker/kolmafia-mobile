package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.KnollRequest] friendly knoll NPCs. */
object KnollRequest {
    const val FLAMING_MUSHROOM = 755
    const val FROZEN_MUSHROOM = 756
    const val STINKY_MUSHROOM = 757

    fun getNpcName(action: String?): String? = when (action) {
        "dk_mayor" -> "Mayor Zapruder"
        "dk_innabox" -> "Innabox"
        "dk_plunger" -> "The Plunger"
        else -> null
    }

    fun parseResponse(
        url: String,
        html: String,
        questDatabase: QuestDatabase?,
        inventoryManager: InventoryManager?,
    ) {
        if (!url.contains("knoll_friendly", ignoreCase = true)) return
        val action = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1) ?: return
        if (action != "dk_mayor") return
        when {
            html.contains("flaming glowsticks", ignoreCase = true) ->
                inventoryManager?.consumeItemLocally(FLAMING_MUSHROOM, 1)
            html.contains("iced-out bling", ignoreCase = true) ->
                inventoryManager?.consumeItemLocally(FROZEN_MUSHROOM, 1)
            html.contains("limburger biker boots", ignoreCase = true) ->
                inventoryManager?.consumeItemLocally(STINKY_MUSHROOM, 1)
        }
        when {
            html.contains("It is fortunate that you have arrived") ->
                questDatabase?.setQuestIfBetter(Quest.BUGBEAR, QuestDatabase.STARTED)
            html.contains("Mayor Zapruder looks at the tiny pitchfork") ->
                questDatabase?.setQuestIfBetter(Quest.BUGBEAR, "step1")
            html.contains("Please, hand me the mushroom") ->
                questDatabase?.setQuestIfBetter(Quest.BUGBEAR, "step2")
            html.contains("The bugbears have finally returned to a state of normalcy") ->
                questDatabase?.setQuestIfBetter(Quest.BUGBEAR, QuestDatabase.FINISHED)
        }
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.contains("place.php", ignoreCase = true) ||
            !url.contains("knoll_friendly", ignoreCase = true)
        ) {
            return false
        }
        val action = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)
        val npc = getNpcName(action)
        if (npc != null) {
            sessionLogger?.appendRawLine("Visiting $npc")
        }
        return true
    }
}
