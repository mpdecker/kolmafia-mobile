package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ThingWithNoNameSync

/** Desktop [net.sourceforge.kolmafia.request.SuburbanDisRequest] suburbandis.php. */
object SuburbanDisRequest {
    fun getAdventuresUsed(url: String): Int =
        if (url.contains("suburbandis.php", ignoreCase = true) &&
            url.contains("action=dothis", ignoreCase = true)
        ) 1 else 0

    fun parseResponse(
        url: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        inventory: InventoryManager?,
        ascensionNumber: Int,
    ) {
        if (!url.contains("suburbandis.php", ignoreCase = true)) return
        val action = Regex("""[?&]action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)
        if (action == null) {
            if (html.contains("Since you carved up the Thing With no Name", ignoreCase = true)) {
                preferences?.setInt("lastThingWithNoNameDefeated", ascensionNumber)
            }
            return
        }
        if (action == "stoned" && html.contains("You acquire an effect", ignoreCase = true)) {
            preferences?.setInt("lastThingWithNoNameDefeated", ascensionNumber)
            val stone1 = Regex("""stone1=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            val stone2 = Regex("""stone2=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            listOfNotNull(stone1, stone2).forEach { id ->
                inventory?.consumeItemLocally(id, 1)
                rewindStoneQuest(id, questDatabase)
            }
        }
    }

    private fun rewindStoneQuest(stoneId: Int, questDatabase: QuestDatabase?) {
        val quest = when (stoneId) {
            ThingWithNoNameSync.FURIOUS_STONE, ThingWithNoNameSync.VANITY_STONE -> Quest.CLUMSINESS
            ThingWithNoNameSync.LECHEROUS_STONE, ThingWithNoNameSync.JEALOUSY_STONE -> Quest.MAELSTROM
            ThingWithNoNameSync.AVARICE_STONE, ThingWithNoNameSync.GLUTTONOUS_STONE -> Quest.GLACIER
            else -> return
        }
        val current = questDatabase?.getProgress(quest) ?: return
        val next = when (current) {
            QuestDatabase.FINISHED -> "step2"
            "step3" -> "step1"
            "step2" -> QuestDatabase.UNSTARTED
            else -> current
        }
        questDatabase.setProgress(quest, next)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("suburbandis.php", ignoreCase = true)
}
