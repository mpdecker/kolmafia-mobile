package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences

object RufusCallChoiceSync {
    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase,
        itemIdForName: (String) -> Int?,
        itemCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (choiceId !in 1497..1498 || preferences == null) return false
        val manager = RufusManager(preferences)
        return if (choiceId == 1497) {
            manager.parseCall(html)
            manager.parseCallResponse(html, decision, questDatabase, itemIdForName, itemCount)
            true
        } else {
            manager.parseCallBackResponse(html, decision, questDatabase, itemIdForName, consumeItem)
            true
        }
    }
}

object ShadowForestChoiceSync {
    fun apply(choiceId: Int, decision: Int, preferences: Preferences?): Boolean {
        if (choiceId != 1500 || decision != 3 || preferences == null) return false
        preferences.setBoolean("_shadowForestLooted", true)
        return true
    }
}

object TreasureHouseChoiceSync {
    fun apply(choiceId: Int, decision: Int, preferences: Preferences?): Boolean {
        if (choiceId != 1493 || decision <= 0 || preferences == null) return false
        preferences.setBoolean("_treasureHouseVisited", true)
        return true
    }
}

object SitTrainingChoiceSync {
    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?): Boolean {
        if (choiceId != 1495 || decision <= 0 || preferences == null) return false
        if (!html.contains("skill", ignoreCase = true) && !html.contains("training", ignoreCase = true)) return false
        preferences.setBoolean("_sitCourseCompleted", true)
        return true
    }
}

object MimicEggDifferentiateChoiceSync {
    private val monsterId = Regex("""(?:[?&])mid=(\d+)""")

    fun apply(
        choiceId: Int,
        choiceUrl: String,
        preferences: Preferences?,
        consumeItem: (Int) -> Unit,
    ): Boolean {
        if (choiceId != 1516 || preferences == null) return false
        val id = monsterId.find(choiceUrl)?.groupValues?.get(1) ?: return false
        val counts = preferences.getString("mimicEggMonsters").split(',')
            .mapNotNull {
                val pair = it.split(':')
                if (pair.size == 2) pair[0] to (pair[1].toIntOrNull() ?: 0) else null
            }.toMap().toMutableMap()
        counts[id] = (counts[id] ?: 0) - 1
        preferences.setString(
            "mimicEggMonsters",
            counts.filterValues { it > 0 }.entries.joinToString(",") { "${it.key}:${it.value}" },
        )
        consumeItem(ItemPool.MIMIC_EGG)
        return true
    }
}

object LoathingIdolChoiceSync {
    fun apply(
        choiceId: Int,
        html: String,
        lastItemUsed: Int?,
        consumeItem: (Int) -> Unit,
        gainItem: (Int) -> Unit,
    ): Boolean {
        if (choiceId != 1505 || !html.contains("You sing:") || lastItemUsed == null) return false
        val replacement = when (lastItemUsed) {
            ItemPool.LOATHING_IDOL_MICROPHONE -> ItemPool.LOATHING_IDOL_MICROPHONE_75
            ItemPool.LOATHING_IDOL_MICROPHONE_75 -> ItemPool.LOATHING_IDOL_MICROPHONE_50
            ItemPool.LOATHING_IDOL_MICROPHONE_50 -> ItemPool.LOATHING_IDOL_MICROPHONE_25
            ItemPool.LOATHING_IDOL_MICROPHONE_25 -> null
            else -> return false
        }
        consumeItem(lastItemUsed)
        replacement?.let(gainItem)
        return true
    }
}

object PhotoBoothLeftoverChoiceSync {
    fun apply(choiceId: Int, preferences: Preferences?): Boolean {
        if (choiceId != 1533 && choiceId != 1536) return false
        preferences?.setBoolean("_photoBoothVisited", true)
        return true
    }
}

object EternityCodpieceChoiceSync {
    private val lost = Regex("""(?is)You lose an item:.*?<b>(.*?)</b>""")
    private val acquired = Regex("""(?is)You acquire an item:.*?<b>(.*?)</b>""")

    fun apply(
        choiceId: Int,
        html: String,
        itemIdForName: (String) -> Int?,
        consumeItem: (Int) -> Unit,
        gainItem: (Int) -> Unit,
        refreshStatus: () -> Unit,
    ): Boolean {
        if (choiceId != 1588) return false
        lost.find(html)?.groupValues?.get(1)?.let(itemIdForName)?.let(consumeItem)
        acquired.find(html)?.groupValues?.get(1)?.let(itemIdForName)?.let(gainItem)
        refreshStatus()
        return true
    }
}

object FleshWorkbenchChoiceSync {
    fun apply(choiceId: Int, decision: Int, preferences: Preferences?): Boolean {
        if (choiceId != 1592 || decision <= 0) return false
        preferences?.setBoolean("_fleshWorkbenchUsed", true)
        return true
    }
}

object AminoSacChoiceSync {
    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?): Boolean {
        if (choiceId != 1593 || decision <= 0 || preferences == null) return false
        if (html.contains("amino", ignoreCase = true)) {
            preferences.setInt("aminoAcidsUsed", minOf(3, preferences.getInt("aminoAcidsUsed") + 1))
        }
        return true
    }
}

object StillSuitChoiceSync {
    private val drams = Regex("""(?:<b>(\d+)</b> drams|Looks like there are (\d+) drams)""")

    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?): Boolean {
        if (choiceId != 1476 || preferences == null) return false
        drams.find(html)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.toIntOrNull()?.let {
            preferences.setInt("familiarSweat", it)
        }
        if (decision == 1 && html.contains("You put your lips to the nozzle")) {
            preferences.setInt("familiarSweat", 0)
            preferences.setString("nextDistillateMods", "")
        }
        return true
    }
}
