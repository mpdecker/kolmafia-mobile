package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RufusManagerTest {

    /** Fresh isolated preferences per call — no cross-test state leakage. */
    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun manager(prefs: Preferences = prefs()) = RufusManager(prefs)

    // Test 1: chooseQuestOption returns 1 for entity when HTML contains entity
    @Test fun chooseQuestOption_entity_returns1() {
        val mgr = manager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "entity") })
        val html = "Choose your quest: entity, artifact, or monument"
        val result = mgr.chooseQuestOption(html)
        assertEquals(1, result)
    }

    // Test 2: chooseQuestOption returns 2 for artifact when HTML contains artifact
    @Test fun chooseQuestOption_artifact_returns2() {
        val mgr = manager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "artifact") })
        val html = "Choose your quest: entity, artifact, or monument"
        val result = mgr.chooseQuestOption(html)
        assertEquals(2, result)
    }

    // Test 3: chooseQuestOption returns 3 for monument when HTML contains monument
    @Test fun chooseQuestOption_monument_returns3() {
        val mgr = manager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "monument") })
        val html = "Choose your quest: entity, artifact, or monument"
        val result = mgr.chooseQuestOption(html)
        assertEquals(3, result)
    }

    // Test 4: chooseQuestOption returns null when type not in HTML
    @Test fun chooseQuestOption_typeNotInHtml_returnsNull() {
        val mgr = manager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "monument") })
        val html = "Choose your quest: entity or artifact"
        val result = mgr.chooseQuestOption(html)
        assertNull(result)
    }

    // Test 5: confirmChoice always returns 1
    @Test fun confirmChoice_always_returns1() {
        val mgr = manager()
        val result = mgr.confirmChoice()
        assertEquals(1, result)
    }

    // Test 6: recordQuestTarget persists to preferences
    @Test fun recordQuestTarget_persistsToPreferences() {
        val testPrefs = prefs()
        val mgr = manager(testPrefs)
        mgr.recordQuestTarget("Shadow Rift Wraith")
        val stored = testPrefs.getString(Preferences.RUFUS_QUEST_TARGET, "")
        assertEquals("Shadow Rift Wraith", stored)
    }

    // Test 7: questType defaults to entity when pref not set
    @Test fun questType_defaultsToEntity() {
        val mgr = manager(prefs())
        val result = mgr.questType
        assertEquals("entity", result)
    }
}
