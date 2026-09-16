package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.data.AdventureQueueDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class AscensionMechanicsPhase7510Test {

    @AfterTest
    fun tearDown() {
        CharpaneValhallaSync.reset()
        ChoiceCombatAshState.reset()
        AdventureQueueDatabase.resetQueue()
    }

    @Test
    fun preAscension_usesDesktopBearArmIdsAndGunpowder() {
        val used = mutableListOf<Int>()
        var pyro = 0
        val counts = mutableMapOf(
            ItemPool.GUNPOWDER to 3,
            ItemPool.LEFT_BEAR_ARM to 1,
            ItemPool.RIGHT_BEAR_ARM to 1,
            ItemPool.BOX_OF_BEAR_ARM to 0,
            ItemPool.GATES_SCROLL to 2,
        )
        ValhallaManager.preAscension(
            ValhallaManager.AscensionDeps(
                preferences = null,
                character = null,
                inventoryCount = { counts[it] ?: 0 },
                useItem = { id, qty ->
                    used += id
                    counts[id] = (counts[id] ?: 0) - qty
                },
                visitPyro = { pyro++ },
            ),
        )
        assertEquals(1, pyro)
        assertTrue(ItemPool.GATES_SCROLL in used)
        assertEquals(ItemPool.LEFT_BEAR_ARM, used.last())
        assertFalse(118 in used)
    }

    @Test
    fun noteGashJump_setsLastBreakfastZero() {
        val prefs = Preferences(MapSettings())
        ValhallaManager.noteGashJump(prefs)
        assertEquals(0, prefs.getInt("lastBreakfast", -1))
    }

    @Test
    fun onAscension_resetsQuestsIslandBugbearBanishesAndCounters() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Quest.RAT.prefKey, QuestDatabase.STARTED)
        prefs.setString(Quest.JIMMY_MUSHROOM.prefKey, QuestDatabase.STARTED)
        prefs.setString(Quest.EVE.prefKey, QuestDatabase.FINISHED)
        prefs.setInt("fratboysDefeated", 9)
        prefs.setInt("statusMedbay", 3)
        prefs.setInt("mothershipProgress", 2)
        TurnCounter.startCounting(prefs, 10, 5, "Bee window begin", "bee.gif")
        val banishes = BanishManager(prefs)
        banishes.banishMonster("Rollover Foo", Banisher.BEANCANNON, currentTurn = 1)
        banishes.banishMonster("Ice Baz", Banisher.ICE_HOUSE, currentTurn = 1)
        val quests = QuestDatabase(prefs)
        var spentReset = 0
        ValhallaManager.onAscension(
            character = KoLCharacter(),
            preferences = prefs,
            banishManager = banishes,
            questDatabase = quests,
            adventureSpentReset = { spentReset++ },
        )
        assertEquals(-1, prefs.getInt("lastBreakfast", 0))
        assertEquals(-1, prefs.getInt("lastGuildStoreOpen", 0))
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.RAT))
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.JIMMY_MUSHROOM))
        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.EVE))
        assertEquals(0, prefs.getInt("fratboysDefeated", -1))
        assertEquals(0, prefs.getInt("statusMedbay", -1))
        assertEquals(0, prefs.getInt("mothershipProgress", -1))
        assertEquals(emptyList(), TurnCounter.load(prefs))
        assertFalse(banishes.isBanished("Rollover Foo", currentTurn = 2))
        assertTrue(banishes.isBanished("Ice Baz", currentTurn = 2))
        assertEquals(1, spentReset)
        assertFalse(CharpaneValhallaSync.inValhalla)
    }

    @Test
    fun postAscension_startsRainWindowsAndAutoQuestTelegram() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoQuest", true)
        val char = KoLCharacter().also { it.setChallengePath("Heavy Rains") }
        val used = mutableListOf<Pair<Int, Int>>()
        var lounge = 0
        ValhallaManager.postAscension(
            ValhallaManager.AscensionDeps(
                preferences = prefs,
                character = char,
                useItem = { id, qty -> used += id to qty },
                visitLounge = { lounge++ },
                visitLoungeFloor2 = { lounge++ },
            ),
        )
        val labels = TurnCounter.load(prefs).map { it.parsedLabel() }
        assertTrue(labels.any { it.contains("Rain Monster window begin") })
        assertTrue(labels.any { it.contains("Rain Monster window end") })
        assertEquals(listOf(ItemPool.SPOOKYRAVEN_TELEGRAM to 1), used)
        assertEquals(2, lounge)
        assertEquals("apathetic", prefs.getString("mood", ""))
    }

    @Test
    fun postAscension_sourceEnlightenmentCapsAtEleven() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("sourcePoints", 40)
        val char = KoLCharacter().also { it.setChallengePath("The Source") }
        ValhallaManager.postAscension(
            ValhallaManager.AscensionDeps(preferences = prefs, character = char),
        )
        assertEquals(11, prefs.getInt("sourceEnlightenment", 0))
    }

    @Test
    fun moonsignCafes_useCanadiaAndGnomadsSigns() {
        val char = KoLCharacter().also { it.setZodiacSign("Platypus") }
        val visited = mutableListOf<String>()
        val reset = mutableListOf<String>()
        ValhallaManager.postAscension(
            ValhallaManager.AscensionDeps(
                preferences = Preferences(MapSettings()),
                character = char,
                visitCafeMenu = { visited += it },
                resetCafeMenu = { reset += it },
            ),
        )
        assertTrue("chezsnootee" in visited)
        assertTrue("hellkitchen" in reset)
        assertTrue("microbrewery" in reset)
        assertTrue(ValhallaManager.canadiaAvailable(char.state.value))
        assertFalse(ValhallaManager.gnomadsAvailable(char.state.value))
    }

    @Test
    fun handleAfterlifeRedirect_defersPostAscensionOnChoice() {
        ChoiceCombatAshState.reset()
        var posted = 0
        ValhallaManager.handleAfterlifeRedirect(
            "choice.php?whichchoice=1",
            ValhallaManager.AscensionDeps(
                preferences = Preferences(MapSettings()),
                character = KoLCharacter(),
                visitLounge = { posted++ },
            ),
        )
        assertEquals(ChoiceCombatAshState.PostChoiceAction.ASCEND, ChoiceCombatAshState.postChoiceAction)
        assertEquals(0, posted)
        assertEquals(
            ChoiceCombatAshState.PostChoiceAction.ASCEND,
            ChoiceCombatAshState.consumePostChoiceAction(),
        )
        assertEquals(ChoiceCombatAshState.PostChoiceAction.NONE, ChoiceCombatAshState.postChoiceAction)
    }

    @Test
    fun pullFreeItems_skipsOwnedVipKey() {
        val pulled = mutableListOf<Int>()
        ValhallaManager.postAscension(
            ValhallaManager.AscensionDeps(
                preferences = Preferences(MapSettings()),
                character = KoLCharacter(),
                inventoryCount = { id -> if (id == ItemPool.VIP_LOUNGE_KEY) 1 else 0 },
                pullFromStorage = { id, _ -> pulled += id },
            ),
        )
        assertFalse(ItemPool.VIP_LOUNGE_KEY in pulled)
        assertTrue(ItemPool.CURSED_KEG in pulled)
        assertTrue(ItemPool.CURSED_MICROWAVE in pulled)
    }
}
