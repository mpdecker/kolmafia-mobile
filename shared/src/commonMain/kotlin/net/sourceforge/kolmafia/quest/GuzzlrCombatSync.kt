package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.math.floor

/** Desktop [FightRequest.updateFinalRoundData] Guzzlr delivery progress + completion hooks. */
object GuzzlrCombatSync {

    const val GUZZLR_SHOES_ID = 10537
    const val GUZZLR_COCKTAIL_SET_ID = 10534
    val GUZZLR_PLATINUM_ITEM_IDS = 10541..10545

    private const val DELIVERY_PROGRESS_PREF = "guzzlrDeliveryProgress"
    private const val DELIVERIES_TODAY_PREF = "_guzzlrDeliveries"

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        locationName: String,
        responseText: String,
        won: Boolean,
        gameDatabase: GameDatabase? = null,
        hasItemEquipped: (Int) -> Boolean = { false },
        hasItemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ) {
        if (questDatabase == null || preferences == null || !won) return
        if (!isQuestStarted(questDatabase)) return

        incrementDeliveryProgress(
            preferences = preferences,
            locationName = locationName,
            responseText = responseText,
            hasItemEquipped = hasItemEquipped,
        )

        completeDelivery(
            text = responseText,
            questDatabase = questDatabase,
            preferences = preferences,
            gameDatabase = gameDatabase,
            hasItemCount = hasItemCount,
            consumeItem = consumeItem,
        )
    }

    fun incrementDeliveryProgress(
        preferences: Preferences?,
        locationName: String,
        responseText: String,
        hasItemEquipped: (Int) -> Boolean = { false },
    ): Boolean {
        val prefs = preferences ?: return false
        if (getProgress(prefs) == QuestDatabase.UNSTARTED) return false

        val questLocation = prefs.getString("guzzlrQuestLocation", "")
        val client = prefs.getString("guzzlrQuestClient", "")
        if (questLocation.isEmpty() || client.isEmpty()) return false
        if (questLocation != locationName) return false
        if (!responseText.contains(client)) return false

        var incr = maxOf(3, 10 - prefs.getInt(DELIVERIES_TODAY_PREF, 0))
        if (hasItemEquipped(GUZZLR_SHOES_ID)) {
            incr = floor(1.5 * incr).toInt()
        }
        prefs.setInt(DELIVERY_PROGRESS_PREF, prefs.getInt(DELIVERY_PROGRESS_PREF, 0) + incr)
        return true
    }

    fun completeDelivery(
        text: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        gameDatabase: GameDatabase? = null,
        hasItemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (!text.contains("You finally manage to track down", ignoreCase = true)) return false

        var tier = preferences?.getString("guzzlrQuestTier", "")?.lowercase().orEmpty()
        val boozeName = preferences?.getString("guzzlrQuestBooze", "").orEmpty()
        val itemId = gameDatabase?.item(boozeName)?.id ?: 0

        if (itemId == GUZZLR_COCKTAIL_SET_ID ||
            boozeName.contains("Guzzlr cocktail", ignoreCase = true)
        ) {
            tier = "platinum"
            for (id in GUZZLR_PLATINUM_ITEM_IDS.reversed()) {
                if (hasItemCount(id) > 0) {
                    consumeItem(id, 1)
                    break
                }
            }
        } else if (itemId > 0) {
            consumeItem(itemId, 1)
        }

        if (tier.isNotBlank()) {
            val deliveryKey = when (tier) {
                "bronze" -> "guzzlrBronzeDeliveries"
                "gold" -> "guzzlrGoldDeliveries"
                "platinum" -> "guzzlrPlatinumDeliveries"
                else -> null
            }
            deliveryKey?.let { key ->
                preferences?.let { prefs ->
                    prefs.setInt(key, prefs.getInt(key, 0) + 1)
                }
            }
        }

        preferences?.setInt(DELIVERIES_TODAY_PREF, (preferences.getInt(DELIVERIES_TODAY_PREF, 0)) + 1)
        preferences?.setInt(DELIVERY_PROGRESS_PREF, 0)
        clearQuestPrefs(preferences)
        questDatabase.setProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED)
        return true
    }

    internal fun clearQuestPrefs(preferences: Preferences?) {
        preferences?.setString("guzzlrQuestBooze", "")
        preferences?.setString("guzzlrQuestClient", "")
        preferences?.setString("guzzlrQuestLocation", "")
        preferences?.setString("guzzlrQuestTier", "")
    }

    private fun isQuestStarted(questDatabase: QuestDatabase): Boolean =
        questDatabase.getProgress(Quest.GUZZLR) != QuestDatabase.UNSTARTED

    private fun getProgress(preferences: Preferences): String =
        preferences.getString(Quest.GUZZLR.prefKey, QuestDatabase.UNSTARTED)
}
