package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
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

    @Test fun handleQuestLog_entityQuest_setsTypeAndTarget() {
        val testPrefs = prefs()
        val mgr = manager(testPrefs)
        val text = "Rufus wants you to go into a Shadow Rift and defeat a shadow scythe."
        assertEquals(QuestDatabase.STARTED, mgr.handleQuestLog(text))
        assertEquals("entity", testPrefs.getString(Preferences.RUFUS_QUEST_TYPE, ""))
        assertEquals("shadow scythe", testPrefs.getString(Preferences.RUFUS_QUEST_TARGET, ""))
    }

    @Test fun handleQuestLog_artifactQuest_setsTypeAndTarget() {
        val testPrefs = prefs()
        val mgr = manager(testPrefs)
        val text = "Rufus wants you to go into a Shadow Rift and find a shadow bucket."
        assertEquals(QuestDatabase.STARTED, mgr.handleQuestLog(text))
        assertEquals("artifact", testPrefs.getString(Preferences.RUFUS_QUEST_TYPE, ""))
        assertEquals("shadow bucket", testPrefs.getString(Preferences.RUFUS_QUEST_TARGET, ""))
    }

    @Test fun handleQuestLog_itemsQuest_resolvesPluralItemName() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 99901,
                name = "wisp of shadow flame",
                descId = "test",
                image = "test.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = "wisps of shadow flame",
            ),
        )
        val testPrefs = prefs()
        val mgr = manager(testPrefs)
        val text = "Rufus wants you to find him 3 wisps of shadow flame from Shadow Rifts."
        assertEquals(
            QuestDatabase.STARTED,
            mgr.handleQuestLog(text, GameDatabase()),
        )
        assertEquals("items", testPrefs.getString(Preferences.RUFUS_QUEST_TYPE, ""))
        assertEquals("wisp of shadow flame", testPrefs.getString(Preferences.RUFUS_QUEST_TARGET, ""))
    }

    @Test fun handleQuestLog_entityDone_returnsStep1() {
        val testPrefs = prefs()
        val mgr = manager(testPrefs)
        val text = "Call Rufus and let him know you defeated that monster."
        assertEquals("step1", mgr.handleQuestLog(text))
        assertEquals("entity", testPrefs.getString(Preferences.RUFUS_QUEST_TYPE, ""))
    }

    @Test fun handleQuestLog_unrelatedText_returnsNull() {
        val mgr = manager(prefs())
        assertNull(mgr.handleQuestLog("Visit the council for more quests."))
    }
}
