package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestManagerTest {
    private data class Fixture(
        val preferences: Preferences,
        val quests: QuestDatabase,
        val context: QuestManager.QuestChangeContext,
    )

    private fun fixture(state: CharacterState = CharacterState()): Fixture {
        val preferences = Preferences(MapSettings())
        val quests = QuestDatabase(preferences)
        return Fixture(
            preferences,
            quests,
            QuestManager.QuestChangeContext(
                preferences = preferences,
                questDatabase = quests,
                characterState = state,
            ),
        )
    }

    @Test
    fun handleQuestChange_routesAdventureBySnarfblat() {
        val f = fixture()
        QuestManager.handleQuestChange(
            "https://www.kingdomofloathing.com/adventure.php?snarfblat=${WhiteCitadelSync.WHITEYS_GROVE}",
            "It's A Sign!",
            f.context,
        )
        assertEquals("step1", f.quests.getProgress(Quest.CITADEL))
    }

    @Test
    fun handleQuestChange_handlesRedirectOnlyPalindome() {
        val f = fixture()
        QuestManager.handleQuestChange(
            "adventure.php?snarfblat=${PalindomeSync.PALINDOME_ADVENTURE}",
            "",
            f.context,
        )
        assertEquals(QuestDatabase.STARTED, f.quests.getProgress(Quest.PALINDOME))
    }

    @Test
    fun handleQuestChange_routesManorThirdFloor() {
        val f = fixture(CharacterState(ascensionNumber = 7))
        QuestManager.handleQuestChange(
            "place.php?whichplace=manor3",
            "Spookyraven Manor Third Floor",
            f.context,
        )
        assertEquals(QuestDatabase.FINISHED, f.quests.getProgress(Quest.SPOOKYRAVEN_NECKLACE))
        assertEquals(QuestDatabase.FINISHED, f.quests.getProgress(Quest.SPOOKYRAVEN_DANCE))
        assertEquals(7, f.preferences.getInt("lastSecondFloorUnlock", 0))
    }

    @Test
    fun handleQuestChange_routesResidualSpeakeasyName() {
        val f = fixture()
        QuestManager.handleQuestChange(
            "place.php?whichplace=town_wrong",
            """Speakeasy <div id=town_speakeasyname title="The Test Tube">""",
            f.context,
        )
        assertEquals("The Test Tube", f.preferences.getString("speakeasyName", ""))
        assertTrue(f.preferences.getBoolean("ownsSpeakeasy", false))
    }

    @Test
    fun handleQuestChange_routesDungeonsUnlock() {
        val f = fixture()
        QuestManager.handleQuestChange("da.php", "A path leads to the barrelshrine.", f.context)
        assertTrue(f.preferences.getBoolean("barrelShrineUnlocked", false))
    }

    @Test
    fun updateQuestData_dispatchesCombatWriters() {
        val f = fixture()
        val result = QuestManager.updateQuestData(
            "WINWINWIN",
            "Boss Bat",
            f.context.copy(won = true),
        )
        assertTrue(result.advanced)
        assertEquals("step4", f.quests.getProgress(Quest.BAT))
    }

    @Test
    fun updateCyrusAdjective_deduplicatesValues() {
        val f = fixture()
        assertTrue(QuestManager.updateCyrusAdjective(QuestItemUsedSync.CA_BASE_PAIR, f.preferences))
        QuestManager.updateCyrusAdjective("stronger", f.preferences)
        assertEquals("stronger", f.preferences.getString("cyrusAdjectives", ""))
    }

    @Test
    fun handleQuestChange_routesDesertBeachPyramidModel() {
        val f = fixture()
        QuestManager.handleQuestChange(
            "place.php?whichplace=desertbeach&action=db_pyramid1",
            "the model bursts into flames and is quickly consumed",
            f.context,
        )
        assertEquals(QuestDatabase.STARTED, f.quests.getProgress(Quest.PYRAMID))
    }

    @Test
    fun handleQuestChange_dispatchesQuestLogPage() {
        var page = 0
        var body = ""
        QuestManager.handleQuestChange(
            "questlog.php?which=3",
            "accomplishments",
            QuestManager.QuestChangeContext(
                parseQuestLogPage = { which, html ->
                    page = which
                    body = html
                },
            ),
        )
        assertEquals(3, page)
        assertEquals("accomplishments", body)
    }
}
