package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuzzlrCombatSyncTest {

    private fun activeGuzzlrPrefs(): Pair<Preferences, QuestDatabase> {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.GUZZLR, "step1")
        prefs.setString("guzzlrQuestClient", "Gerald")
        prefs.setString("guzzlrQuestLocation", "The Sleazy Back Alley")
        return prefs to db
    }

    @Test
    fun incrementDeliveryProgress_questNotStarted_noIncrement() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("guzzlrQuestClient", "Gerald")
        prefs.setString("guzzlrQuestLocation", "The Sleazy Back Alley")
        assertFalse(
            GuzzlrCombatSync.incrementDeliveryProgress(
                preferences = prefs,
                locationName = "The Sleazy Back Alley",
                responseText = "Gerald waves at you.",
            ),
        )
        assertEquals(0, prefs.getInt("guzzlrDeliveryProgress", 0))
    }

    @Test
    fun incrementDeliveryProgress_wrongLocation_noIncrement() {
        val (prefs, _) = activeGuzzlrPrefs()
        assertFalse(
            GuzzlrCombatSync.incrementDeliveryProgress(
                preferences = prefs,
                locationName = "The Spooky Forest",
                responseText = "Gerald waves at you.",
            ),
        )
        assertEquals(0, prefs.getInt("guzzlrDeliveryProgress", 0))
    }

    @Test
    fun incrementDeliveryProgress_matchingLocationAndClient_incrementsByTenWhenFresh() {
        val (prefs, _) = activeGuzzlrPrefs()
        assertTrue(
            GuzzlrCombatSync.incrementDeliveryProgress(
                preferences = prefs,
                locationName = "The Sleazy Back Alley",
                responseText = "You spot Gerald near a dumpster.",
            ),
        )
        assertEquals(10, prefs.getInt("guzzlrDeliveryProgress", 0))
    }

    @Test
    fun incrementDeliveryProgress_manyDeliveriesToday_usesMinimumIncrement() {
        val (prefs, _) = activeGuzzlrPrefs()
        prefs.setInt("_guzzlrDeliveries", 7)
        GuzzlrCombatSync.incrementDeliveryProgress(
            preferences = prefs,
            locationName = "The Sleazy Back Alley",
            responseText = "Gerald nods approvingly.",
        )
        assertEquals(3, prefs.getInt("guzzlrDeliveryProgress", 0))
    }

    @Test
    fun incrementDeliveryProgress_guzzlrShoesEquipped_appliesBonus() {
        val (prefs, _) = activeGuzzlrPrefs()
        GuzzlrCombatSync.incrementDeliveryProgress(
            preferences = prefs,
            locationName = "The Sleazy Back Alley",
            responseText = "Gerald cheers.",
            hasItemEquipped = { id -> id == GuzzlrCombatSync.GUZZLR_SHOES_ID },
        )
        assertEquals(15, prefs.getInt("guzzlrDeliveryProgress", 0))
    }

    @Test
    fun completeDelivery_bronzeTier_incrementsCountersAndResetsQuest() {
        val (prefs, db) = activeGuzzlrPrefs()
        prefs.setString("guzzlrQuestTier", "bronze")
        prefs.setString("guzzlrQuestBooze", "bottle of rum")
        prefs.setInt("guzzlrDeliveryProgress", 9)
        var consumedId = -1
        val gameDb = object : GameDatabase() {
            override fun item(name: String) = ItemData(
                1234, name, "", "", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null,
            )
        }
        assertTrue(
            GuzzlrCombatSync.completeDelivery(
                text = "You finally manage to track down Gerald and deliver the booze.",
                questDatabase = db,
                preferences = prefs,
                gameDatabase = gameDb,
                consumeItem = { id, qty ->
                    consumedId = id
                    assertEquals(1, qty)
                },
            ),
        )
        assertEquals(1234, consumedId)
        assertEquals(1, prefs.getInt("guzzlrBronzeDeliveries", 0))
        assertEquals(1, prefs.getInt("_guzzlrDeliveries", 0))
        assertEquals(0, prefs.getInt("guzzlrDeliveryProgress", -1))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GUZZLR))
        assertEquals("", prefs.getString("guzzlrQuestClient", "x"))
    }

    @Test
    fun completeDelivery_platinumCocktailSet_consumesHighestOwnedCocktail() {
        val (prefs, db) = activeGuzzlrPrefs()
        prefs.setString("guzzlrQuestTier", "platinum")
        prefs.setString("guzzlrQuestBooze", "Guzzlr cocktail set")
        var consumedId = -1
        val gameDb = object : GameDatabase() {
            override fun item(name: String) = ItemData(
                GuzzlrCombatSync.GUZZLR_COCKTAIL_SET_ID,
                name, "", "", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null,
            )
        }
        assertTrue(
            GuzzlrCombatSync.completeDelivery(
                text = "You finally manage to track down Pat.",
                questDatabase = db,
                preferences = prefs,
                gameDatabase = gameDb,
                hasItemCount = { id ->
                    when (id) {
                        10541 -> 1
                        10543 -> 2
                        else -> 0
                    }
                },
                consumeItem = { id, _ -> consumedId = id },
            ),
        )
        assertEquals(10543, consumedId)
        assertEquals(1, prefs.getInt("guzzlrPlatinumDeliveries", 0))
    }

    @Test
    fun applyCombatWin_loss_doesNothing() {
        val (prefs, db) = activeGuzzlrPrefs()
        GuzzlrCombatSync.applyCombatWin(
            questDatabase = db,
            preferences = prefs,
            locationName = "The Sleazy Back Alley",
            responseText = "Gerald laughs at your defeat.",
            won = false,
        )
        assertEquals(0, prefs.getInt("guzzlrDeliveryProgress", 0))
    }

    @Test
    fun applyCombatWin_win_incrementsAndCompletes() {
        val (prefs, db) = activeGuzzlrPrefs()
        prefs.setString("guzzlrQuestTier", "bronze")
        prefs.setString("guzzlrQuestBooze", "bottle of rum")
        GuzzlrCombatSync.applyCombatWin(
            questDatabase = db,
            preferences = prefs,
            locationName = "The Sleazy Back Alley",
            responseText = "Gerald says hi. You finally manage to track down Gerald.",
            won = true,
            gameDatabase = object : GameDatabase() {
                override fun item(name: String) = ItemData(
                    999, name, "", "", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null,
                )
            },
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GUZZLR))
        assertEquals(1, prefs.getInt("guzzlrBronzeDeliveries", 0))
    }
}
