package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.Gender
import net.sourceforge.kolmafia.data.ZoneCombatCalculator
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AlliedHqRequestHub
import net.sourceforge.kolmafia.request.DimemasterRequestHub
import net.sourceforge.kolmafia.request.FlowerTradeinRequestHub
import net.sourceforge.kolmafia.request.QuartersmasterRequestHub
import net.sourceforge.kolmafia.request.RumpleRequestHub
import net.sourceforge.kolmafia.session.AdventureSpentTracker

class GameRuntimeLibraryPhase5230Test {

    @Test
    fun revision_phase5290() {
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun oilPeak_mlGatesWeight() {
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Oil Peak",
                monster = "oil slick",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(monsterLevel = 10),
            ),
        )
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Oil Peak",
                monster = "oil slick",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(monsterLevel = 25),
            ),
        )
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Oil Peak",
                monster = "oil cartel",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(monsterLevel = 100),
            ),
        )
    }

    @Test
    fun fratBattlefield_clearedOnlyBoss() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("hippiesDefeated", 1000)
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Battlefield (Frat Uniform)",
                monster = "The Big Wisniewski",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Battlefield (Frat Uniform)",
                monster = "War Hippy Infantryman",
                weighting = 2,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
    }

    @Test
    fun fratBattlefield_greenOpsAt401() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("hippiesDefeated", 401)
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Battlefield (Frat Uniform)",
                monster = "Green Ops Soldier",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
        prefs.setInt("hippiesDefeated", 400)
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Battlefield (Frat Uniform)",
                monster = "Green Ops Soldier",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
    }

    @Test
    fun fcle_genderGatesClingyPirate() {
        val male = CharacterState(gender = Gender.MALE)
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The F'c'le",
                monster = "clingy pirate (female)",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(characterState = male),
            ),
        )
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The F'c'le",
                monster = "clingy pirate (male)",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(characterState = male),
            ),
        )
    }

    @Test
    fun ziggurat_lianaClearedAfterTurns() {
        val prefs = Preferences(MapSettings())
        val tracker = AdventureSpentTracker(prefs)
        repeat(3) { tracker.addTurn("A Massive Ziggurat") }
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "A Massive Ziggurat",
                monster = "dense liana",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(adventureSpent = tracker),
            ),
        )
    }

    @Test
    fun nsContest_bossWhenOneLeft() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("nsContestants1", 1)
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Fastest Adventurer Contest",
                monster = "Tasmanian Dervish",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "Fastest Adventurer Contest",
                monster = "generic contestant",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(preferences = prefs),
            ),
        )
    }

    @Test
    fun nemesisLair_classGates() {
        val sc = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Nemesis' Lair",
                monster = "hellseal guardian",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(characterState = sc),
            ),
        )
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Nemesis' Lair",
                monster = "warehouse worker",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(characterState = sc),
            ),
        )
    }

    @Test
    fun slimeTube_mlGates() {
        assertEquals(
            1,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Slime Tube",
                monster = "Slime Hand",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(monsterLevel = 200),
            ),
        )
        assertEquals(
            0,
            ZoneCombatCalculator.adjustConditionalWeighting(
                zone = "The Slime Tube",
                monster = "Slime",
                weighting = 1,
                ctx = ZoneCombatCalculator.Context(monsterLevel = 200),
            ),
        )
    }

    @Test
    fun http_hubs_phase5290() {
        assertTrue(DimemasterRequestHub.registerRequest("shop.php?whichshop=dimemaster"))
        assertFalse(DimemasterRequestHub.registerRequest("adventure.php"))
        assertTrue(QuartersmasterRequestHub.registerRequest("shop.php?whichshop=quartersmaster"))
        assertTrue(FlowerTradeinRequestHub.registerRequest("shop.php?whichshop=flowertradein"))
        assertTrue(AlliedHqRequestHub.registerRequest("shop.php?whichshop=alliedhq"))
        assertTrue(RumpleRequestHub.registerRequest("shop.php?whichshop=rumple"))
    }
}
