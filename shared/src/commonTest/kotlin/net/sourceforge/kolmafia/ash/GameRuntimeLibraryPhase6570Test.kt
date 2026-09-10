package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.adventure.choice.ChoiceWalkAway
import net.sourceforge.kolmafia.adventure.prep.AdventureZoneGates
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.ZoneParentDatabase
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.TavernManager

/**
 * Focused XLV Track A coverage (phases 6551–6570).
 * Revision bump deferred to parent (phase6670); stays phase6670.
 */
class GameRuntimeLibraryPhase6570Test {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
        AdventurePrep.resetForTest()
        MaximizerContinuation.forceContinue()
        TavernManager.overrideSquare = -1
        runBlocking { ZoneParentDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
        AdventurePrep.resetForTest()
        MaximizerContinuation.forceContinue()
        TavernManager.overrideSquare = -1
    }

    @Test
    fun revisionStaysPhase6550() {
        assertEquals("phase6670", GameRuntimeLibrary.REVISION)
        assertEquals("6670", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun canAdventure_noneAndBlankAreFalse() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(adventures = "10"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(can_adventure(to_location("none"))));""").trim(),
        )
        assertFalse(AdventurePrep.canAdventureAt("", CharacterState(adventuresLeft = 5)))
        assertTrue(AdventurePrep.isNoneLocation("NONE"))
        assertTrue(AdventurePrep.isNoneLocation("none"))
    }

    @Test
    fun canAdventure_removedRootZoneIsFalse() {
        val cs = CharacterState(adventuresLeft = 10, level = 10)
        val crimbo = AdventureZone(
            zoneName = "Crimbo25",
            urlParams = "adventure=595",
            locationName = "Smoldering Bone Spikes",
            environment = "outdoor",
            diffLevel = "mid",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertTrue(LimitModeGates.getRootZone("Crimbo25").equals("Removed", ignoreCase = true))
        assertFalse(AdventurePrep.canAdventureAt("Smoldering Bone Spikes", cs, crimbo))
    }

    @Test
    fun canAdventure_tavernCellarNeedsRatQuest() {
        val cs = CharacterState(adventuresLeft = 10, level = 10)
        val prefs = Preferences(MapSettings())
        val cellar = AdventureZone(
            zoneName = "Town",
            urlParams = "cellar",
            locationName = "The Typical Tavern Cellar",
            environment = "indoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(
            AdventurePrep.canAdventureAt("The Typical Tavern Cellar", cs, cellar, prefs),
        )
        prefs.setString(Quest.RAT.prefKey, QuestDatabase.STARTED)
        assertTrue(
            AdventurePrep.canAdventureAt("The Typical Tavern Cellar", cs, cellar, prefs),
        )
    }

    @Test
    fun prepareForAdventure_noneIsFalse_blankLastLocIsTrue() {
        assertEquals(
            "false",
            outputLib(
                GameRuntimeLibrary(),
                """print(to_string(prepare_for_adventure(to_location("none"))));""",
            ).trim(),
        )
        assertEquals(
            "true",
            outputLib(GameRuntimeLibrary(), "print(to_string(prepare_for_adventure()));").trim(),
        )
    }

    @Test
    fun adventure_zeroTurnsAndUnknownLoc_continueValue() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(adventure(0, to_location("The Haunted Pantry"))));""")
                .trim(),
        )
        MaximizerContinuation.abort()
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(adventure(0, to_location("The Haunted Pantry"))));""")
                .trim(),
        )
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun choiceFightFlags_syncFromState() {
        ChoiceCombatAshState.reset()
        assertEquals("false", outputLib(GameRuntimeLibrary(), "print(to_string(in_multi_fight()));").trim())
        assertEquals("false", outputLib(GameRuntimeLibrary(), "print(to_string(fight_follows_choice()));").trim())
        assertEquals("false", outputLib(GameRuntimeLibrary(), "print(to_string(choice_follows_fight()));").trim())

        ChoiceCombatAshState.noteChoiceVisit(1076, "mayo")
        ChoiceCombatAshState.noteChoiceDecision(1, """<a href="fight.php">""")
        assertTrue(ChoiceCombatAshState.fightFollowsChoice)
        assertEquals("true", outputLib(GameRuntimeLibrary(), "print(to_string(fight_follows_choice()));").trim())

        ChoiceCombatAshState.noteFightEnd("""<a href="choice.php">Continue</a>""")
        assertTrue(ChoiceCombatAshState.choiceFollowsFight)
        assertEquals("true", outputLib(GameRuntimeLibrary(), "print(to_string(choice_follows_fight()));").trim())

        assertTrue(ChoiceWalkAway.canWalkFromChoice(1543))
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1601))
        assertEquals("true", outputLib(GameRuntimeLibrary(), "print(to_string(can_walk_from_choice()));").trim())
    }

    @Test
    fun tavern_unknownGoalAndAbortReturnMinusOne() {
        val prefs = Preferences(MapSettings())
        prefs.setString("tavernLayout", "0000000000003000000000000")
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("13", outputLib(lib, """print(tavern("faucet"));""").trim())
        assertEquals("-1", outputLib(lib, """print(tavern("not-a-goal"));""").trim())
        MaximizerContinuation.abort()
        assertEquals("-1", outputLib(lib, "print(tavern());").trim())
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun preValidate_springBreakAirportGate() {
        val ctx = AdventurePrep.buildContext(CharacterState(adventuresLeft = 5), Preferences(MapSettings()))
        val zone = AdventureZone(
            zoneName = "Spring Break Beach",
            urlParams = "adventure=1",
            locationName = "Spring Break Beach",
            environment = "outdoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(AdventureZoneGates.preValidateAdventure("Spring Break Beach", zone, ctx))
    }
}
