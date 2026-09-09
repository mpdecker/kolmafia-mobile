package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.ZoneCombatCalculator
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.Crimbo20BoozeRequest
import net.sourceforge.kolmafia.request.FriarRequestHub
import net.sourceforge.kolmafia.request.GourdRequestHub
import net.sourceforge.kolmafia.request.SafetyShelterRequest
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.EncounterManager

class GameRuntimeLibraryPhase5170Test {

    @Test
    fun revision_phase5290() {
        assertEquals("phase5890", GameRuntimeLibrary.REVISION)
        assertEquals("phase5890", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun saberForceZone_detectsMonsterInCombatTable() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("_saberForceMonsterCount", 3)
        prefs.setString("_saberForceMonster", "spooky vampire")
        assertTrue(EncounterManager.isSaberForceZone("The Spooky Forest", prefs))
        assertFalse(EncounterManager.isSaberForceZone("The Boss Bat's Lair", prefs))
        assertEquals(
            100.0,
            ZoneCombatCalculator.areaCombatPercent(
                CombatDatabase.getByLocation("The Spooky Forest")!!,
                stateful = true,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
            0.001,
        )
    }

    @Test
    fun fungalNethers_classGatesWeight() {
        val sc = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        val w = ZoneCombatCalculator.adjustConditionalWeighting(
            zone = "The Fungal Nethers",
            monster = "muscular mushroom guy",
            weighting = 1,
            ctx = ZoneCombatCalculator.Context(characterState = sc),
        )
        assertEquals(1, w)
        val zero = ZoneCombatCalculator.adjustConditionalWeighting(
            zone = "The Fungal Nethers",
            monster = "muscular mushroom guy",
            weighting = 1,
            ctx = ZoneCombatCalculator.Context(
                characterState = CharacterState(characterClass = CharacterClass.PASTAMANCER.id),
            ),
        )
        assertEquals(0, zero)
    }

    @Test
    fun bossBat_conditionalWeightUsesTurns() {
        val prefs = Preferences(MapSettings())
        val tracker = AdventureSpentTracker(prefs)
        repeat(5) { tracker.addTurn("The Boss Bat's Lair") }
        val boss = ZoneCombatCalculator.adjustConditionalWeighting(
            zone = "The Boss Bat's Lair",
            monster = "Boss Bat",
            weighting = 0,
            ctx = ZoneCombatCalculator.Context(adventureSpent = tracker),
        )
        assertEquals(1, boss)
        val other = ZoneCombatCalculator.adjustConditionalWeighting(
            zone = "The Boss Bat's Lair",
            monster = "batrat",
            weighting = 1,
            ctx = ZoneCombatCalculator.Context(adventureSpent = tracker),
        )
        assertEquals(1, other)
    }

    @Test
    fun moonlightHelpers_matchDesktopFormula() {
        val ronald = KolGameHolidayCalendar.ronaldMoonlight()
        assertTrue(ronald in 0..4)
        val grimace = KolGameHolidayCalendar.grimaceMoonlight()
        assertTrue(grimace in 0..4)
    }

    @Test
    fun closet_amount_int_overload() {
        assertEquals("0", outputLib(GameRuntimeLibrary(), """print(closet_amount(1));""").trim())
    }

    @Test
    fun http_hubs_phase5290() {
        assertTrue(FriarRequestHub.registerRequest("friars.php?action=blessing"))
        assertFalse(FriarRequestHub.registerRequest("adventure.php"))
        assertTrue(GourdRequestHub.registerRequest("town_right.php?place=gourd"))
        assertTrue(Crimbo20BoozeRequest.registerRequest("shop.php?whichshop=crimbo20booze"))
        assertTrue(SafetyShelterRequest.registerRequest("place.php?whichplace=falloutshelter"))
    }
}
