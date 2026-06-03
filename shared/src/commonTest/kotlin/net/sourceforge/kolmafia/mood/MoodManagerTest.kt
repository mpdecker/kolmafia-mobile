package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoodManagerTest {

    private fun prefs(autoBuffEnabled: Boolean = true): Preferences {
        val s = MapSettings()
        s.putBoolean(Preferences.AUTO_BUFF, autoBuffEnabled)
        return Preferences(s)
    }

    private fun trigger(effectId: Int, skillId: Int, minTurns: Int = 1) =
        MoodTrigger(effectId, "Effect $effectId", skillId, "Skill $skillId", minTurns)

    private fun effect(id: Int, duration: Int) =
        EffectData(id = id, name = "Effect $id", duration = duration)

    private fun effectState(vararg effects: EffectData) =
        EffectState(effects = effects.toList())

    // ── missingTriggers ──────────────────────────────────────────────────────

    @Test fun missingTriggers_effectPresent_returnsEmpty() {
        val mood = Mood("test", listOf(trigger(effectId = 10, skillId = 200, minTurns = 1)))
        val effects = effectState(effect(10, duration = 3))
        val missing = MoodManager.missingTriggers(mood, effects)
        assertTrue(missing.isEmpty())
    }

    @Test fun missingTriggers_effectAbsent_returnsTrigger() {
        val t = trigger(effectId = 10, skillId = 200)
        val mood = Mood("test", listOf(t))
        val missing = MoodManager.missingTriggers(mood, effectState())
        assertEquals(listOf(t), missing)
    }

    @Test fun missingTriggers_effectBelowMinTurns_returnsTrigger() {
        val t = trigger(effectId = 10, skillId = 200, minTurns = 5)
        val mood = Mood("test", listOf(t))
        val effects = effectState(effect(10, duration = 3))
        val missing = MoodManager.missingTriggers(mood, effects)
        assertEquals(listOf(t), missing)
    }

    @Test fun missingTriggers_exactlyAtMinTurns_returnsEmpty() {
        val t = trigger(effectId = 10, skillId = 200, minTurns = 3)
        val mood = Mood("test", listOf(t))
        val effects = effectState(effect(10, duration = 3))
        assertTrue(MoodManager.missingTriggers(mood, effects).isEmpty())
    }

    @Test fun missingTriggers_multipleTriggers_returnsMissingOnly() {
        val t1 = trigger(effectId = 10, skillId = 200)
        val t2 = trigger(effectId = 20, skillId = 300)
        val mood = Mood("test", listOf(t1, t2))
        val effects = effectState(effect(10, duration = 5))  // only t1 present
        assertEquals(listOf(t2), MoodManager.missingTriggers(mood, effects))
    }
}
