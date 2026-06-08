package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MoodManagerAtSongTest {

    private fun prefs(): Preferences = Preferences(MapSettings().also {
        it.putBoolean(Preferences.AUTO_BUFF, true)
    })

    private fun fakeCastSkillManager(): SkillManager {
        val fakeClient = io.ktor.client.HttpClient(MockEngine { _ -> respond("") })
        val fakeRequest = net.sourceforge.kolmafia.skill.SkillCastRequest(fakeClient)
        val fakeEventBus = net.sourceforge.kolmafia.event.GameEventBus()
        return object : SkillManager(fakeClient, fakeRequest, fakeEventBus) {
            override suspend fun cast(
                skill: net.sourceforge.kolmafia.skill.SkillData,
                quantity: Int,
            ): Result<Unit> = Result.success(Unit)
        }
    }

    private fun makeManager(): MoodManager =
        MoodManager(skillManager = fakeCastSkillManager(), preferences = prefs(), uneffectRequest = null)

    // ── atSongLimit tests ──────────────────────────────────────────────────────

    @Test fun atSongLimit_isThreeForAccordionThief() {
        val state = CharacterState(characterClass = CharacterClass.ACCORDION_THIEF.id)
        assertEquals(3, state.atSongLimit)
    }

    @Test fun atSongLimit_isZeroForSealClubber() {
        val state = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        assertEquals(0, state.atSongLimit)
    }

    @Test fun atSongLimit_isZeroForSauceror() {
        val state = CharacterState(characterClass = CharacterClass.SAUCEROR.id)
        assertEquals(0, state.atSongLimit)
    }

    @Test fun atSongLimit_isZeroForDiscoBandit() {
        val state = CharacterState(characterClass = CharacterClass.DISCO_BANDIT.id)
        assertEquals(0, state.atSongLimit)
    }

    @Test fun atSongLimit_isZeroForTurtleTamer() {
        val state = CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id)
        assertEquals(0, state.atSongLimit)
    }

    // ── isAtSong tests ─────────────────────────────────────────────────────────

    @Test fun isAtSong_returnsFalseForClearlyNonSongEffect() {
        // "Strength of Ten Ettins" is a buff, not an AT song, regardless of DB state
        // When EffectDatabase is empty (test env), getByName returns null → false
        val mgr = makeManager()
        assertFalse(mgr.isAtSong("Strength of Ten Ettins"))
    }

    @Test fun isAtSong_returnsFalseForUnknownEffect() {
        val mgr = makeManager()
        assertFalse(mgr.isAtSong("This Effect Does Not Exist In Any Database"))
    }

    // ── executeActiveMood: non-AT class never triggers eviction ───────────────

    @Test fun executeActiveMood_nonAtClass_doesNotEvict() = runBlocking {
        var uneffectCalled = false
        val fakeUneffect = object : net.sourceforge.kolmafia.request.UneffectRequest(
            io.ktor.client.HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun uneffect(effectId: Int): Result<Unit> {
                uneffectCalled = true
                return Result.success(Unit)
            }
        }
        val mgr = MoodManager(
            skillManager = fakeCastSkillManager(),
            preferences = prefs(),
            uneffectRequest = fakeUneffect,
        )
        mgr.activeMood = Mood("test", listOf(
            MoodTrigger(effectId = 60, effectName = "Aloysius' Antiphon of Aptitude",
                skillId = 6003, skillName = "Aloysius' Antiphon of Aptitude", minimumTurns = 5)
        ))
        val charState = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id) // not AT
        mgr.executeActiveMood(EffectState(effects = emptyList()), SkillState(), charState)
        assertFalse(uneffectCalled, "Non-AT class should never trigger eviction")
    }
}
