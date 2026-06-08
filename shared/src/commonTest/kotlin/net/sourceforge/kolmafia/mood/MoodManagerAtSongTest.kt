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
import kotlin.test.assertTrue

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

    @Test fun executeActiveMood_doesNotEvictWhenSlotsNotFull() = runBlocking {
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
            MoodTrigger(60, "Aloysius' Antiphon of Aptitude", 6003, "Aloysius' Antiphon of Aptitude", 5),
            MoodTrigger(61, "The Moxious Madrigal",          6004, "The Moxious Madrigal", 5),
            MoodTrigger(63, "Polka of Plenty",                6006, "The Polka of Plenty", 5),
        ))
        val charState = CharacterState(characterClass = CharacterClass.ACCORDION_THIEF.id)
        // Only 2 active songs — adding 3rd, slot not full (limit=3)
        val activeEffects = listOf(
            net.sourceforge.kolmafia.effect.EffectData(id = 60, name = "Aloysius' Antiphon of Aptitude", duration = 10),
            net.sourceforge.kolmafia.effect.EffectData(id = 61, name = "The Moxious Madrigal", duration = 10),
        )
        mgr.executeActiveMood(EffectState(effects = activeEffects), SkillState(), charState)
        assertFalse(uneffectCalled, "Should not evict when 2 songs active and limit is 3")
    }

    /** Creates a MoodManager whose isAtSong() returns true for the given effect names. */
    private fun songMoodManager(
        songEffectNames: Set<String>,
        prefs: Preferences = this.prefs(),
        onUneffect: ((Int) -> Unit)? = null,
    ): MoodManager {
        val skillMgr = fakeCastSkillManager()
        val uneffect = onUneffect?.let { cb ->
            object : net.sourceforge.kolmafia.request.UneffectRequest(
                io.ktor.client.HttpClient(MockEngine { respond("") })
            ) {
                override suspend fun uneffect(effectId: Int): Result<Unit> {
                    cb(effectId)
                    return Result.success(Unit)
                }
            }
        }
        return object : MoodManager(skillMgr, prefs, uneffect) {
            override fun isAtSong(effectName: String): Boolean = effectName in songEffectNames
        }
    }

    /** Builds a SkillState with free-cast (mpCost=0, unlimited) skills for the given id→name pairs. */
    private fun skillStateFor(vararg idName: Pair<Int, String>): SkillState =
        SkillState(skills = idName.map { (id, name) ->
            net.sourceforge.kolmafia.skill.SkillData(
                id = id, name = name,
                type = net.sourceforge.kolmafia.skill.SkillType.BUFF,
                mpCost = 0, dailyLimit = 0, timesCast = 0,
            )
        })

    @Test fun executeActiveMood_evictsLowestPrioritySongWhenSlotFull() = runBlocking {
        val evictedIds = mutableListOf<Int>()
        val songNames = setOf(
            "Aloysius' Antiphon of Aptitude",
            "The Moxious Madrigal",
            "Polka of Plenty",
            "Fat Leon's Phat Loot Lyric",
        )
        val mgr = songMoodManager(songNames, onUneffect = { evictedIds.add(it) })

        // Mood: 4 AT song triggers in priority order (index 0 = highest)
        mgr.activeMood = Mood("test", listOf(
            MoodTrigger(60, "Aloysius' Antiphon of Aptitude", 6003, "Aloysius' Antiphon of Aptitude", 5),
            MoodTrigger(61, "The Moxious Madrigal",          6004, "The Moxious Madrigal", 5),
            MoodTrigger(63, "Polka of Plenty",                6006, "The Polka of Plenty", 5),
            MoodTrigger(67, "Fat Leon's Phat Loot Lyric",    6010, "Fat Leon's Phat Loot Lyric", 5),
        ))

        val charState = CharacterState(characterClass = CharacterClass.ACCORDION_THIEF.id)  // limit=3
        val skillState = skillStateFor(6010 to "Fat Leon's Phat Loot Lyric")

        // 3 songs active (at limit); 4th (Fat Leon's) missing → trigger fires
        val activeEffects = listOf(
            net.sourceforge.kolmafia.effect.EffectData(60, "Aloysius' Antiphon of Aptitude", 10),
            net.sourceforge.kolmafia.effect.EffectData(61, "The Moxious Madrigal", 10),
            net.sourceforge.kolmafia.effect.EffectData(63, "Polka of Plenty", 10),
        )
        mgr.executeActiveMood(EffectState(effects = activeEffects), skillState, charState)

        // Should evict Polka of Plenty (effectId=63) — last in trigger list among the 3 actives
        assertEquals(listOf(63), evictedIds,
            "Should evict effect ID 63 (Polka of Plenty — lowest priority in trigger list)")
    }

    @Test fun executeActiveMood_doesNotDoubleEvictWhenTwoSongsTrigger() = runBlocking {
        val evictedIds = mutableListOf<Int>()
        val songNames = setOf("Song A", "Song B", "Song C", "Song D", "Song E")
        val mgr = songMoodManager(songNames, onUneffect = { evictedIds.add(it) })

        // Mood: 5 song triggers; first 2 are "new priority" (missing); slots full (3/3)
        mgr.activeMood = Mood("test", listOf(
            MoodTrigger(1, "Song A", 1001, "Skill A", 5),
            MoodTrigger(2, "Song B", 1002, "Skill B", 5),
            MoodTrigger(3, "Song C", 1003, "Skill C", 5),
            MoodTrigger(4, "Song D", 1004, "Skill D", 5),
            MoodTrigger(5, "Song E", 1005, "Skill E", 5),
        ))

        val charState = CharacterState(characterClass = CharacterClass.ACCORDION_THIEF.id)  // limit=3
        val skillState = skillStateFor(1001 to "Skill A", 1002 to "Skill B")

        // 3 songs active (C, D, E = full slot); A and B are missing → 2 triggers fire
        val activeEffects = listOf(
            net.sourceforge.kolmafia.effect.EffectData(3, "Song C", 10),
            net.sourceforge.kolmafia.effect.EffectData(4, "Song D", 10),
            net.sourceforge.kolmafia.effect.EffectData(5, "Song E", 10),
        )
        mgr.executeActiveMood(EffectState(effects = activeEffects), skillState, charState)

        // Two triggers fire (A and B both missing). Each evicts a different song.
        // First trigger: evict Song E (effectId=5, last in list among actives)
        // Second trigger: evict Song D (effectId=4, now lowest after E removed locally)
        // Should NOT evict Song E twice
        assertEquals(2, evictedIds.size, "Should evict exactly 2 songs for 2 new song triggers")
        assertTrue(5 in evictedIds, "Song E (id=5) should be evicted first")
        assertTrue(4 in evictedIds, "Song D (id=4) should be evicted second")
        assertFalse(evictedIds.distinct().size < evictedIds.size, "No song should be evicted twice")
    }
}
