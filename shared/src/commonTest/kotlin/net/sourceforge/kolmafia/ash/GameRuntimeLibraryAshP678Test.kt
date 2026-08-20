package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.WoodsDemonChoiceSync

class GameRuntimeLibraryAshP678Test {

    @Test
    fun revision_phase683() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun clumsiness_startAndBossChoice() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(WoodsDemonChoiceSync.apply(560, 1, db, prefs))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CLUMSINESS))
        WoodsDemonChoiceSync.apply(561, 1, db, prefs)
        assertEquals("step1", db.getProgress(Quest.CLUMSINESS))
        assertEquals(WoodsDemonChoiceSync.THORAX, prefs.getString("clumsinessGroveBoss"))
        WoodsDemonChoiceSync.apply(560, 2, db, prefs)
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.CLUMSINESS))
    }

    @Test
    fun clumsiness_mettleUsesVanityStone() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            WoodsDemonChoiceSync.apply(563, 1, db, prefs, itemCount = { id ->
                if (id == WoodsDemonChoiceSync.VANITY_STONE) 1 else 0
            }),
        )
        assertEquals("step3", db.getProgress(Quest.CLUMSINESS))
        assertEquals(WoodsDemonChoiceSync.THORAX, prefs.getString("clumsinessGroveBoss"))
        assertFalse(WoodsDemonChoiceSync.apply(563, 2, db, prefs))
    }

    @Test
    fun maelstromAndGlacier_bossChains() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        WoodsDemonChoiceSync.apply(565, 2, db, prefs)
        assertEquals("step1", db.getProgress(Quest.MAELSTROM))
        assertEquals(WoodsDemonChoiceSync.THUGS, prefs.getString("maelstromOfLoversBoss"))
        WoodsDemonChoiceSync.apply(568, 1, db, prefs)
        assertEquals("step1", db.getProgress(Quest.GLACIER))
        assertEquals(WoodsDemonChoiceSync.MAMMON, prefs.getString("glacierOfJerksBoss"))
        WoodsDemonChoiceSync.apply(
            569, 1, db, prefs,
            itemCount = { 0 },
        )
        assertEquals("step3", db.getProgress(Quest.GLACIER))
        assertEquals(WoodsDemonChoiceSync.SNITCH, prefs.getString("glacierOfJerksBoss"))
    }

    @Test
    fun questChoiceRules_wires561() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 561,
                responseText = "You Must Choose Your Destruction!",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.CLUMSINESS))
        assertEquals(WoodsDemonChoiceSync.BAT_IN_THE_SPATS, prefs.getString("clumsinessGroveBoss"))
    }
}
