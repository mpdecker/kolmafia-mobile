package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Neverending Party Fair choices 1322–1328.
 */
object PartyFairChoiceSync {

    const val BEGINNING = 1322
    const val ALL_DONE = 1323
    const val PAUSED = 1324
    const val ROOM_WITH_A_VIEW = 1325
    const val GONE_KITCHIN = 1326
    const val FORWARD_TO_THE_BACK = 1327
    const val BASEMENT_URGES = 1328

    const val PARTY_HARD_T_SHIRT = 9944
    const val JAM_BAND_BOOTLEG = 9968
    const val VERY_SMALL_RED_DRESS = 9963
    const val PURPLE_BEAST_ENERGY_DRINK = 9958
    const val ELECTRONICS_KIT = 9952

    private val SAFE_PATTERN = Regex("""find ([\d,]+) Meat in the safe""")
    private val TRASH_PATTERN = Regex("""must have been (\d+) pieces of trash""")
    private val GERALDINE_PATTERN =
        Regex("""Geraldine wants (\d+)<table>.*?descitem\((\d+)\)""", RegexOption.DOT_MATCHES_ALL)
    private val GERALD_PATTERN =
        Regex("""Gerald wants (\d+)<table>.*?descitem\((\d+)\)""", RegexOption.DOT_MATCHES_ALL)

    fun applyVisit(choiceId: Int, html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        return when (choiceId) {
            BEGINNING -> {
                val quest = when {
                    html.contains("talk to him and help him get more booze") -> "booze"
                    html.contains("Think you can help me clean the place up?") -> "trash"
                    html.contains("helping her with whatever problem she's having with the snacks") -> "food"
                    html.contains("megawoots right now") -> "woots"
                    html.contains("taking up a collection from the guests") -> "dj"
                    html.contains("all of the people to leave") -> "partiers"
                    else -> return false
                }
                prefs.setString("_questPartyFairQuest", quest)
                true
            }
            PAUSED -> {
                prefs.setInt("encountersUntilNEPChoice", 7)
                true
            }
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        hasItemEquipped: (Int) -> Boolean = { false },
        itemCount: (Int) -> Int = { 0 },
        itemIdFromDesc: (String) -> Int? = { null },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        resyncQuestLogPage1: () -> Unit = {},
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        return when (choiceId) {
            BEGINNING -> applyBeginning(
                decision, questDatabase, preferences, hasItemEquipped, resyncQuestLogPage1,
            )
            ALL_DONE -> {
                questDatabase.setProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED)
                preferences.setString("_questPartyFairQuest", "")
                preferences.setString("_questPartyFairProgress", "")
                true
            }
            PAUSED -> {
                if (decision == 5) return false
                preferences.setInt(
                    "encountersUntilNEPChoice",
                    (preferences.getInt("encountersUntilNEPChoice", 0) - 1).coerceAtLeast(0),
                )
                preferences.setInt(
                    "_neverendingPartyFreeTurns",
                    (preferences.getInt("_neverendingPartyFreeTurns", 0) + 1).coerceAtMost(10),
                )
                true
            }
            ROOM_WITH_A_VIEW -> applyRoomWithAView(decision, html, questDatabase, preferences, consumeItem)
            GONE_KITCHIN -> applyGoneKitchin(
                decision, html, questDatabase, preferences, itemCount, itemIdFromDesc, consumeItem,
            )
            FORWARD_TO_THE_BACK -> applyForwardToTheBack(
                decision, html, questDatabase, preferences, itemCount, itemIdFromDesc, consumeItem,
            )
            BASEMENT_URGES -> {
                if (decision != 4) return false
                incrementWoots(preferences, questDatabase)
                consumeItem(ELECTRONICS_KIT, 1)
                true
            }
            else -> false
        }
    }

    private fun applyBeginning(
        decision: Int,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        hasItemEquipped: (Int) -> Boolean,
        resyncQuestLogPage1: () -> Unit,
    ): Boolean {
        val hard = hasItemEquipped(PARTY_HARD_T_SHIRT)
        preferences.setBoolean("_partyHard", hard)
        when (decision) {
            1 -> {
                preferences.setInt("encountersUntilNEPChoice", 7)
                val quest = preferences.getString("_questPartyFairQuest", "")
                if (quest == "booze" || quest == "food") {
                    questDatabase.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
                    preferences.setString("_questPartyFairProgress", "")
                } else {
                    questDatabase.setProgress(Quest.PARTY_FAIR, "step1")
                    when (quest) {
                        "woots" -> preferences.setInt("_questPartyFairProgress", 10)
                        "partiers" -> preferences.setInt("_questPartyFairProgress", if (hard) 100 else 50)
                        "dj" -> preferences.setInt("_questPartyFairProgress", if (hard) 10000 else 5000)
                        "trash" -> resyncQuestLogPage1()
                    }
                }
            }
            2 -> {
                preferences.setString("_questPartyFair", "")
                preferences.setString("_questPartyFairQuest", "")
                preferences.setString("_questPartyFairProgress", "")
            }
            else -> return true
        }
        return true
    }

    private fun applyRoomWithAView(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        when (decision) {
            3 -> {
                val current = preferences.getInt("_questPartyFairProgress", 0)
                preferences.setInt(
                    "_questPartyFairProgress",
                    current - kotlin.math.floor(current * 0.3).toInt(),
                )
                consumeItem(JAM_BAND_BOOTLEG, 1)
            }
            4 -> {
                val meat = SAFE_PATTERN.find(html)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")?.toIntOrNull() ?: return false
                val next = (preferences.getInt("_questPartyFairProgress", 0) - meat).coerceAtLeast(0)
                preferences.setInt("_questPartyFairProgress", next)
                if (next < 1) {
                    questDatabase.setProgress(Quest.PARTY_FAIR, "step2")
                }
            }
            5 -> {
                incrementWoots(preferences, questDatabase)
                consumeItem(VERY_SMALL_RED_DRESS, 1)
            }
            else -> return false
        }
        return true
    }

    private fun applyGoneKitchin(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        itemCount: (Int) -> Int,
        itemIdFromDesc: (String) -> Int?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        when (decision) {
            3 -> applyGeraldNeed(html, GERALDINE_PATTERN, questDatabase, preferences, itemCount, itemIdFromDesc)
            4 -> {
                consumeStoredItem(preferences, 10, consumeItem)
                questDatabase.setQuestIfBetter(Quest.PARTY_FAIR, QuestDatabase.FINISHED)
                preferences.setString("_questPartyFairQuest", "")
                preferences.setString("_questPartyFairProgress", "")
            }
            5 -> {
                val trash = TRASH_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
                preferences.setInt(
                    "_questPartyFairProgress",
                    (preferences.getInt("_questPartyFairProgress", 0) - trash).coerceAtLeast(0),
                )
            }
            else -> return false
        }
        return true
    }

    private fun applyForwardToTheBack(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        itemCount: (Int) -> Int,
        itemIdFromDesc: (String) -> Int?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        when (decision) {
            3 -> applyGeraldNeed(html, GERALD_PATTERN, questDatabase, preferences, itemCount, itemIdFromDesc)
            4 -> {
                val qty = if (preferences.getBoolean("_partyHard", false)) 20 else 10
                consumeStoredItem(preferences, qty, consumeItem)
                questDatabase.setQuestIfBetter(Quest.PARTY_FAIR, QuestDatabase.FINISHED)
                preferences.setString("_questPartyFairQuest", "")
                preferences.setString("_questPartyFairProgress", "")
            }
            5 -> {
                val current = preferences.getInt("_questPartyFairProgress", 0)
                preferences.setInt(
                    "_questPartyFairProgress",
                    current - kotlin.math.floor(current * 0.2).toInt(),
                )
                consumeItem(PURPLE_BEAST_ENERGY_DRINK, 1)
            }
            else -> return false
        }
        return true
    }

    private fun applyGeraldNeed(
        html: String,
        pattern: Regex,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        itemCount: (Int) -> Int,
        itemIdFromDesc: (String) -> Int?,
    ) {
        val match = pattern.find(html)
        if (match != null) {
            val count = match.groupValues[1].toIntOrNull() ?: 0
            val itemId = itemIdFromDesc(match.groupValues[2]) ?: 0
            preferences.setString("_questPartyFairProgress", "$count $itemId")
            if (itemId > 0 && itemCount(itemId) >= count) {
                questDatabase.setProgress(Quest.PARTY_FAIR, "step2")
            }
        }
        questDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1")
    }

    private fun consumeStoredItem(
        preferences: Preferences,
        quantity: Int,
        consumeItem: (Int, Int) -> Unit,
    ) {
        val pref = preferences.getString("_questPartyFairProgress", "")
        val space = pref.indexOf(' ')
        if (space > 0) {
            pref.substring(space).trim().toIntOrNull()?.let { consumeItem(it, quantity) }
        }
    }

    private fun incrementWoots(preferences: Preferences, questDatabase: QuestDatabase) {
        val next = (preferences.getInt("_questPartyFairProgress", 0) + 20).coerceAtMost(100)
        preferences.setInt("_questPartyFairProgress", next)
        if (next == 100) {
            questDatabase.setProgress(Quest.PARTY_FAIR, "step2")
        }
    }
}
