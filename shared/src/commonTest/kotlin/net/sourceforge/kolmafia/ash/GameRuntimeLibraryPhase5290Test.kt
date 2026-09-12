package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.AdventureQueueDatabase
import net.sourceforge.kolmafia.data.ZoneCombatCalculator
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.EdShopRequestHub
import net.sourceforge.kolmafia.request.TicketCounterRequestHub
import net.sourceforge.kolmafia.session.EncounterManager
import net.sourceforge.kolmafia.session.FightIotmSync

class GameRuntimeLibraryPhase5290Test {

    @Test
    fun revision_phase5290() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun telegram_drunkCowpoke_stepMatrix() {
        val prefs = Preferences(MapSettings())
        prefs.setString("lttQuestName", "Sheriff Wanted")
        prefs.setString("questLTTQuestByWire", "step1")
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Investigating a Plaintive Telegram",
                monster = "drunk cowpoke",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
        prefs.setString("questLTTQuestByWire", "step2")
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Investigating a Plaintive Telegram",
                monster = "drunk cowpoke",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
    }

    @Test
    fun shadowRift_ingressRejectsWrongMonster() {
        val prefs = Preferences(MapSettings())
        prefs.setString("shadowRiftIngress", "manor3")
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Shadow Rift",
                monster = "shadow bat",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
        assertEquals(
            -4,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Shadow Rift",
                monster = "shadow cow",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
    }

    @Test
    fun gingerbread_sewersGatePigeon() {
        val prefs = Preferences(MapSettings())
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Gingerbread Civic Center",
                monster = "gingerbread pigeon",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
        prefs.setBoolean("gingerSewersUnlocked", true)
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Gingerbread Civic Center",
                monster = "gingerbread pigeon",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
    }

    @Test
    fun canadianWildlife_yuleHoundGate() {
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Canadian Wildlife Preserve",
                monster = "wild reindeer",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(familiarId = 0),
            ),
        )
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Canadian Wildlife Preserve",
                monster = "wild reindeer",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(familiarId = 269),
            ),
        )
    }

    @Test
    fun fitzsimmons_hatchOpenSwapsMonsters() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_lastFitzsimmonsHatch", 10)
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Wreck of the Edgar Fitzsimmons",
                monster = "cargo crab",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs, turnsPlayed = 15),
            ),
        )
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Wreck of the Edgar Fitzsimmons",
                monster = "mine crab",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs, turnsPlayed = 15),
            ),
        )
    }

    @Test
    fun holdHands_skillWritesPrefs() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FightIotmSync.applyHoldHands(
                html = "You stop the battle for a moment and hold hands with you.",
                preferences = prefs,
                monsterName = "spooky vampire",
                locationName = "The Spooky Forest",
            ),
        )
        assertEquals("spooky vampire", prefs.getString("holdHandsMonster", ""))
        assertEquals(3, prefs.getInt("holdHandsMonsterCount", 0))
        assertEquals("The Spooky Forest", prefs.getString("holdHandsLocation", ""))
    }

    @Test
    fun holdHands_decrementsOnMatch() {
        val prefs = Preferences(MapSettings())
        prefs.setString("holdHandsMonster", "spooky vampire")
        prefs.setString("holdHandsLocation", "The Spooky Forest")
        prefs.setInt("holdHandsMonsterCount", 3)
        assertTrue(EncounterManager.isHoldHandsMonster(prefs, "spooky vampire", "The Spooky Forest"))
        FightIotmSync.applyHoldHands(
            html = "You win the fight!",
            preferences = prefs,
            monsterName = "spooky vampire",
            locationName = "The Spooky Forest",
        )
        assertEquals(2, prefs.getInt("holdHandsMonsterCount", 0))
    }

    @Test
    fun adventureQueue_enqueueCanonicalizesThe() {
        AdventureQueueDatabase.resetQueue()
        AdventureQueueDatabase.enqueue("The Spooky Forest", "The spooky vampire")
        val q = AdventureQueueDatabase.getZoneQueue("The Spooky Forest")
        assertTrue(q.isNotEmpty())
        // Canonicalization strips leading "the " when MonsterDatabase has no match,
        // or resolves to the database name when present.
        assertEquals(
            AdventureQueueDatabase.canonicalizeMonsterName("The spooky vampire"),
            q.last(),
        )
    }

    @Test
    fun http_hubs_phase5290() {
        assertTrue(TicketCounterRequestHub.registerRequest("shop.php?whichshop=arcade"))
        assertFalse(TicketCounterRequestHub.registerRequest("adventure.php"))
        assertTrue(EdShopRequestHub.registerRequest("shop.php?whichshop=edunder_shopshop"))
    }
}
