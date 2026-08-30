package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class SorceressLairSyncTest {
    private fun prefs() = Preferences(MapSettings())

    @Test
    fun prismLiberatesKingAndFinishesFinalQuest() {
        val prefs = prefs()
        val quests = QuestDatabase(prefs)
        quests.setProgress(Quest.FINAL, "step12")
        var liberated = false

        assertTrue(
            SorceressLairSync.parseTowerResponse(
                action = "ns_11_prism",
                html = "King Ralph the XI stands before you in all his regal glory",
                questDatabase = quests,
                preferences = prefs,
                setKingLiberated = { liberated = true },
            ),
        )

        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.FINAL))
        assertTrue(prefs.getBoolean("kingLiberated", false))
        assertTrue(liberated)
    }

    @Test
    fun ordinaryTowerResponseDelegatesToTowerParser() {
        val prefs = prefs()
        val quests = QuestDatabase(prefs)

        assertTrue(
            SorceressLairSync.parseTowerResponse(
                action = "ns_05_monster1",
                html = "<img src='nstower_tower2.gif'>",
                questDatabase = quests,
                preferences = prefs,
            ),
        )
        assertEquals("step7", quests.getProgress(Quest.FINAL))
    }

    @Test
    fun enteringSorceressFightRetainsOnlyConfidence() {
        val manager = EffectManager(HttpClient(MockEngine { respondOk() }), GameEventBus())
        manager.replaceEffectsForTest(
            listOf(
                EffectData(1791, "Confidence!", -1),
                EffectData(1, "Beaten Up", 5),
                EffectData(2, "Some Intrinsic", -1),
            ),
        )

        assertTrue(SorceressLairSync.enterSorceressFight(manager))
        assertEquals(listOf("Confidence!"), manager.state.value.effects.map { it.name })
    }

    @Test
    fun contestVisitParsesChallengeAndStartsQuest() {
        val prefs = prefs()
        val quests = QuestDatabase(prefs)
        val html = "You feel optimistic about your chances in the Strongest Adventurer contest"

        assertTrue(SorceressLairSync.visitChoice(1003, html, prefs, quests))
        assertEquals("Muscle", prefs.getString("nsChallenge1", ""))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.FINAL))
    }

    @Test
    fun hedgeVisitTracksRoomAndLastAdventure() {
        val prefs = prefs()
        val quests = QuestDatabase(prefs)
        var lastAdventure = ""

        assertTrue(
            SorceressLairSync.visitChoice(
                1008,
                "You take cold damage.",
                prefs,
                quests,
                setLastAdventure = { lastAdventure = it },
            ),
        )
        assertEquals("The Hedge Maze", lastAdventure)
        assertEquals(4, prefs.getInt("currentHedgeMazeRoom", 0))
        assertEquals("cold", prefs.getString("nsChallenge4", ""))
    }

    @Test
    fun challengeNamesAndDescriptionsShareTelescopeTables() {
        assertEquals("Crowd #1", TelescopeSync.getChallengeName(0))
        assertEquals("Maze Trap #2", TelescopeSync.getChallengeName(4))
        assertEquals("Fastest Adventurer", TelescopeSync.getChallengeDescription(0, ""))
        assertEquals("Coldest Adventurer", TelescopeSync.getChallengeDescription(2, "cold"))
        assertEquals("(unknown)", TelescopeSync.getChallengeDescription(3, "unknown"))
    }

    @Test
    fun nagamarPreflightSkipsBeecoreAndExistingWand() {
        assertFalse(SorceressLairSync.needsNagamar("ns_10_sorcfight", inBeecore = true, hasWand = false))
        assertFalse(SorceressLairSync.needsNagamar("ns_10_sorcfight", inBeecore = false, hasWand = true))
        assertTrue(SorceressLairSync.needsNagamar("ns_10_sorcfight", inBeecore = false, hasWand = false))
        assertFalse(SorceressLairSync.needsNagamar("ns_09_monster5", inBeecore = false, hasWand = false))
    }
}
