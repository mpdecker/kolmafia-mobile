package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences

object ExploathingCouncilChoiceSync {
    fun apply(choiceId: Int, html: String, setKingLiberated: () -> Unit): Boolean {
        if (choiceId != 1389 || !html.contains("free King Ralph", ignoreCase = true)) return false
        setKingLiberated()
        return true
    }
}

object IslandWarRationChoiceSync {
    private val tossId = Regex("""(?:[?&])tossid=(\d+)""")
    private val frat = Regex("""(?is)(.*?)\s+(?:defeats?|takes out)\s+(\d+)\s+frat boys""")
    private val hippy = Regex("""(?is)(.*?)\s+(?:defeats?|takes out)\s+(\d+)\s+hippies""")

    fun apply(
        choiceId: Int,
        choiceUrl: String,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int) -> Unit,
    ): Boolean {
        if (choiceId != 1391) return false
        val itemId = tossId.find(choiceUrl)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        consumeItem(itemId)
        val casualty = frat.find(html)?.let { "fratboysDefeated" to it }
            ?: hippy.find(html)?.let { "hippiesDefeated" to it }
        if (casualty != null && preferences != null) {
            val (key, match) = casualty
            preferences.setInt(key, preferences.getInt(key, 0) + match.groupValues[2].toInt())
        }
        return true
    }
}

object DecorateTentChoiceSync {
    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int) -> Unit,
    ): Boolean {
        if (choiceId != 1392 || preferences == null) return false
        val success = when (decision) {
            1 -> html.contains("camouflage patterns")
            2 -> html.contains("magical symbols")
            3 -> html.contains("sweet skull and crossbones")
            else -> false
        }
        if (!success) return false
        consumeItem(ItemPool.BURNT_STICK)
        preferences.setInt("campAwayDecoration", decision)
        return true
    }
}

object SmokeSignalChoiceSync {
    fun apply(choiceId: Int, decision: Int, html: String, consumeItem: (Int) -> Unit): Boolean {
        if (choiceId != 1394 || decision != 1 ||
            !html.contains("You send a smoky message to the sky.")
        ) return false
        consumeItem(ItemPool.CAMPFIRE_SMOKE)
        return true
    }
}

object CrimboSpiritChoiceSync {
    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?): Boolean {
        if (choiceId != 1439 || decision <= 0 || preferences == null ||
            !html.contains("Crimbo Spirit", ignoreCase = true)
        ) return false
        preferences.setBoolean("_crimboSpiritChoiceUsed", true)
        return true
    }
}

object GovernmentShipmentChoiceSync {
    fun apply(choiceId: Int, html: String, consumeItem: (Int) -> Unit): Boolean {
        val itemId = when (choiceId) {
            1442 -> ItemPool.GOVERNMENT_FOOD_SHIPMENT
            1443 -> ItemPool.GOVERNMENT_BOOZE_SHIPMENT
            1444 -> ItemPool.GOVERNMENT_CANDY_SHIPMENT
            else -> return false
        }
        if (!html.contains("You fill out all the appropriate forms")) return false
        consumeItem(itemId)
        return true
    }
}

object GuzzlrChoiceSync {
    private val quest = Regex(
        """(?is)(Bronze|Gold|Platinum).*?(?:client|deliver(?:y|ing)?(?:\s+to)?)\s*<b>?([^<,.]+)</b>?.*?(?:in|at)\s+<b>?([^<.]+)</b>""",
    )
    private val descId = Regex("""descitem\((?:'|")?([^'"),]+)""")

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase,
        itemIdFromDesc: (String) -> Int?,
        itemName: (Int) -> String?,
        itemCount: (Int) -> Int,
        resyncQuestLog: () -> Unit,
    ): Boolean {
        if (choiceId != 1412 || decision !in 2..4 || preferences == null) return false
        val parsed = quest.find(html)
        val tier = parsed?.groupValues?.get(1)?.lowercase()
            ?: when (decision) { 2 -> "bronze"; 3 -> "gold"; else -> "platinum" }
        parsed?.let {
            preferences.setString("guzzlrQuestClient", it.groupValues[2].trim())
            preferences.setString("guzzlrQuestLocation", it.groupValues[3].trim())
        }
        preferences.setString("guzzlrQuestTier", tier)
        if (tier != "bronze") {
            val key = "_guzzlr${tier.replaceFirstChar { it.uppercase() }}Deliveries"
            preferences.setInt(key, preferences.getInt(key, 0) + 1)
        }
        val boozeId = descId.find(html)?.groupValues?.get(1)?.let(itemIdFromDesc)
        if (boozeId != null) itemName(boozeId)?.let { preferences.setString("guzzlrQuestBooze", it) }
        if (preferences.getString("guzzlrQuestBooze").isBlank() ||
            preferences.getString("guzzlrQuestLocation").isBlank()
        ) resyncQuestLog()
        questDatabase.setProgress(
            Quest.GUZZLR,
            if (boozeId != null && itemCount(boozeId) > 0) "step1" else QuestDatabase.STARTED,
        )
        return true
    }
}

object AfterAvatarChoiceSync {
    val choiceIds = setOf(1344, 1409, 1465, 1496, 1524, 1554, 1595)
    private val classes = listOf(
        "Seal Clubber", "Turtle Tamer", "Pastamancer",
        "Sauceror", "Disco Bandit", "Accordion Thief",
    )

    fun apply(choiceId: Int, decision: Int, resetAfterAvatar: (String) -> Unit): Boolean {
        if (choiceId !in choiceIds || decision !in 1..6) return false
        resetAfterAvatar(classes[decision - 1])
        return true
    }
}

object GreyYouLabChoiceSync {
    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (choiceId !in 1457..1460 || !html.contains("You acquire an item")) return false
        when (choiceId) {
            1457 -> consumeItem(ItemPool.GOOIFIED_ANIMAL_MATTER, 5)
            1458 -> consumeItem(ItemPool.GOOIFIED_VEGETABLE_MATTER, 5)
            1459 -> consumeItem(ItemPool.GOOIFIED_MINERAL_MATTER, 5)
            1460 -> {
                val costs = when (decision) {
                    1 -> intArrayOf(30, 0, 0); 2 -> intArrayOf(0, 30, 0)
                    3 -> intArrayOf(0, 0, 30); 4 -> intArrayOf(15, 15, 0)
                    5 -> intArrayOf(0, 15, 15); 6 -> intArrayOf(15, 0, 15)
                    7 -> intArrayOf(10, 10, 10); else -> return false
                }
                listOf(
                    ItemPool.GOOIFIED_ANIMAL_MATTER,
                    ItemPool.GOOIFIED_VEGETABLE_MATTER,
                    ItemPool.GOOIFIED_MINERAL_MATTER,
                ).forEachIndexed { index, item -> if (costs[index] > 0) consumeItem(item, costs[index]) }
            }
        }
        return true
    }
}

object SiteAlphaLabChoiceSync {
    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        consumeItem: (Int) -> Unit,
    ): Boolean {
        if (choiceId != 1461 || preferences == null) return false
        when (decision) {
            1 -> preferences.setInt("primaryLabGooIntensity", preferences.getInt("primaryLabGooIntensity") + 1)
            2 -> preferences.setInt("primaryLabGooIntensity", preferences.getInt("primaryLabGooIntensity") - 1)
            3 -> consumeItem(ItemPool.GREY_GOO_RING)
            5 -> preferences.setBoolean("primaryLabCheerCoreGrabbed", true)
            else -> return false
        }
        return true
    }
}
