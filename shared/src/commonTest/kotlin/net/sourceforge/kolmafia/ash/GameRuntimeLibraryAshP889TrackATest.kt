package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.track.TrackManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP889TrackATest {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
    }

    @Test
    fun phase889_currentRoundAndHandlingChoice() {
        ChoiceCombatAshState.currentRound = 3
        ChoiceCombatAshState.handlingChoice = true
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("3", outputLib(lib, "print(current_round());"))
        assertEquals("true", outputLib(lib, "print(handling_choice());"))
    }

    @Test
    fun phase890_lastChoiceAndDecision() {
        ChoiceCombatAshState.lastChoice = 1420
        ChoiceCombatAshState.lastDecision = 2
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("1420", outputLib(lib, "print(last_choice());"))
        assertEquals("2", outputLib(lib, "print(last_decision());"))
    }

    @Test
    fun phase891_availableChoiceOptions() {
        ChoiceCombatAshState.lastChoiceResponseText = """
            <form>
            <input type="hidden" name="whichchoice" value="100">
            <input type="submit" name="option" value="1">Take the left path
            <input type="submit" name="option" value="2">Take the right path
            </form>
        """.trimIndent()
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("2", outputLib(lib, "print(count(available_choice_options()));"))
        assertTrue(outputLib(lib, "print(available_choice_options()[1]);").contains("left", ignoreCase = true))
    }

    @Test
    fun phase892_runChoiceReturnsLastWhenOptionZero() {
        ChoiceCombatAshState.handlingChoice = true
        ChoiceCombatAshState.lastChoice = 100
        ChoiceCombatAshState.lastChoiceResponseText = "choice-html"
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("choice-html", outputLib(lib, "print(run_choice(0));"))
    }

    @Test
    fun phase893_runCombatAndRunTurnBuffers() {
        ChoiceCombatAshState.currentRound = 2
        ChoiceCombatAshState.lastFightResponseText = "fight-html"
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("fight-html", outputLib(lib, "print(run_combat());"))
        assertEquals("fight-html", outputLib(lib, "print(run_turn());"))
    }

    @Test
    fun phase894_throwAndRunawayReturnBuffers() {
        ChoiceCombatAshState.lastFightResponseText = "fight"
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("fight", outputLib(lib, "print(runaway());"))
        assertEquals("fight", outputLib(lib, """print(throw_item(to_item("none")));"""))
    }

    @Test
    fun phase895_banishedByAndTrackedBy() {
        val p = prefs()
        val banishes = BanishManager(p).also {
            it.banishMonster("spooky vampire", Banisher.BANISHING_SHOUT, 10)
        }
        TrackManager.trackMonster(p, "spooky vampire", TrackManager.Tracker.OLFACTION, 10)
        val lib = GameRuntimeLibrary(preferences = p, banishManager = banishes)
        assertEquals("1", outputLib(lib, """print(count(banished_by("spooky vampire")));"""))
        assertEquals("1", outputLib(lib, """print(count(tracked_by("spooky vampire")));"""))
    }

    @Test
    fun phase896_combatSkillAndStun() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(name = "Tester", classId = "1"))
        }
        val client = HttpClient(MockEngine { respond("ok") })
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus()).also {
            it.learnLocalSkill(SkillData(10003, "Club Foot", SkillType.COMBAT, 0, 0, 0))
        }
        val lib = GameRuntimeLibrary(preferences = prefs(), character = char, skillManager = skills)
        assertEquals("true", outputLib(lib, """print(combat_skill_available(to_skill("Club Foot")));"""))
        assertEquals("Club Foot", outputLib(lib, "print(stun_skill());"))
    }

    @Test
    fun phase897_trackALiveSurface() {
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
        ChoiceCombatAshState.currentRound = 1
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("1", outputLib(lib, "print(current_round());"))
        assertEquals("false", outputLib(lib, "print(handling_choice());"))
    }
}
