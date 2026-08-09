package net.sourceforge.kolmafia.mood

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState

class AtSongEvictionTest {

    private val isSong: (String) -> Boolean = { name -> name.startsWith("Song ") }

    @BeforeTest
    fun setUp() {
        runBlocking { SkillDefinitionDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    private fun trigger(effectId: Int, name: String, skillId: Int) =
        MoodTrigger(
            effectId = effectId,
            effectName = name,
            skillId = skillId,
            skillName = name,
            minimumTurns = 5,
        )

    @Test fun isThiefBuffTrigger_trueForAccordionThiefSongSkillId() {
        assertTrue(AtSongEviction.isThiefBuffTrigger(trigger(6010, "Fat Leon's Phat Loot Lyric", 6010)))
    }

    @Test fun isThiefBuffTrigger_falseForNonAtSkillId() {
        assertFalse(AtSongEviction.isThiefBuffTrigger(trigger(1, "Song A", 1001)))
    }

    @Test fun computePrePassEvictionIds_evictsOrphanWhenNeedingNewSong() {
        val effects = EffectState(
            effects = listOf(
                EffectData(10, "Song Orphan A", 10),
                EffectData(11, "Song Orphan B", 10),
                EffectData(12, "Song Orphan C", 10),
            ),
        )
        val moodTriggers = listOf(
            trigger(67, "Song D", 6010),
        )
        val evicted = AtSongEviction.computePrePassEvictionIds(
            effectState = effects,
            moodTriggers = moodTriggers,
            songLimit = 3,
            isAtSong = isSong,
        )
        assertEquals(listOf(10), evicted, "Pre-pass evicts first orphan song not in mood")
    }

    @Test fun computePrePassEvictionIds_noEvictionWhenAllMoodSongsActive() {
        val effects = EffectState(
            effects = listOf(
                EffectData(60, "Song A", 10),
                EffectData(61, "Song B", 10),
                EffectData(63, "Song C", 10),
            ),
        )
        val moodTriggers = listOf(
            trigger(60, "Song A", 6003),
            trigger(61, "Song B", 6004),
            trigger(63, "Song C", 6006),
        )
        val evicted = AtSongEviction.computePrePassEvictionIds(
            effectState = effects,
            moodTriggers = moodTriggers,
            songLimit = 3,
            isAtSong = isSong,
        )
        assertTrue(evicted.isEmpty(), "No thiefNeed when every mood thief song is already active")
    }

    @Test fun computePrePassEvictionIds_skipsAlreadyEvicted() {
        val effects = EffectState(
            effects = listOf(
                EffectData(10, "Song Orphan A", 10),
                EffectData(11, "Song Orphan B", 10),
                EffectData(12, "Song Orphan C", 10),
                EffectData(13, "Song Orphan D", 10),
            ),
        )
        val moodTriggers = listOf(trigger(67, "Song D", 6010))
        val evicted = AtSongEviction.computePrePassEvictionIds(
            effectState = effects,
            moodTriggers = moodTriggers,
            songLimit = 3,
            isAtSong = isSong,
            locallyEvicted = setOf(10),
        )
        assertEquals(listOf(11), evicted)
    }

    @Test fun lowestPriorityActiveSong_prefersLastInMoodTriggerList() {
        val active = listOf(
            EffectData(60, "Song A", 10),
            EffectData(61, "Song B", 10),
            EffectData(63, "Song C", 10),
        )
        val moodTriggers = listOf(
            trigger(60, "Song A", 6003),
            trigger(61, "Song B", 6004),
            trigger(63, "Song C", 6006),
        )
        val lowest = AtSongEviction.lowestPriorityActiveSong(active, moodTriggers)
        assertEquals(63, lowest?.id)
    }
}
