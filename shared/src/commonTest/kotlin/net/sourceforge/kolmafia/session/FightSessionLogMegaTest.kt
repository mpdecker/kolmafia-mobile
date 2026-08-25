package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

class FightSessionLogTest {

    private lateinit var prefs: Preferences
    private lateinit var logger: SessionLogger

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        logger = SessionLogger(prefs, GameEventBus())
        ChoiceCombatAshState.reset()
    }

    @Test
    fun roundPrefixMidFight() {
        assertEquals("Round 3: ", FightSessionLog.roundPrefix(3))
    }

    @Test
    fun roundPrefixAfterBattle() {
        assertEquals("After Battle: ", FightSessionLog.roundPrefix(0))
    }

    @Test
    fun logTextUsesRoundPrefix() {
        ChoiceCombatAshState.currentRound = 2
        FightSessionLog.logText("You deal 10 damage.", logger)
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("Round 2: You deal 10 damage.") })
    }

    @Test
    fun applyLogsAttributeGainsAfterBattle() {
        FightSessionLog.apply(
            html = "<p>You gain 5 Muscle.</p>",
            sessionLogger = logger,
            won = true,
            fightEnded = true,
            monsterName = "spooky mummy",
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("After Battle: You gain 5 Muscle") })
        assertTrue(lines.any { it.contains("spooky mummy wins the fight!") })
    }

    @Test
    fun applyLogsSpecialDamage() {
        ChoiceCombatAshState.currentRound = 4
        FightSessionLog.apply(
            html = "<p>Your opponent continues to bleed.</p>",
            sessionLogger = logger,
            fightEnded = false,
        )
        assertTrue(logger.recentLines().any { it.startsWith("Round 4:") && it.contains("continues to bleed") })
    }
}

class FightProcessPSyncTest {

    private lateinit var prefs: Preferences
    private lateinit var logger: SessionLogger

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        logger = SessionLogger(prefs, GameEventBus())
        ChoiceCombatAshState.currentRound = 1
    }

    @Test
    fun macroErrorPref() {
        assertTrue(
            FightProcessPSync.apply(
                "Macro Abort: out of MP",
                prefs,
                sessionLogger = logger,
            ),
        )
        assertTrue(prefs.getString("lastMacroError", "").contains("Macro Abort"))
    }

    @Test
    fun carnivorousAndBatWingsAndCurveball() {
        FightProcessPSync.apply("Your potted plant swallows your foe.", prefs, sessionLogger = logger)
        assertEquals(1, prefs.getInt("_carnivorousPottedPlantWins", 0))

        FightProcessPSync.apply("You flap your bat wings gustily and fly.", prefs)
        assertEquals(1, prefs.getInt("_batWingsFreeFights", 0))

        prefs.setInt("_curveballFightsLeft", 3)
        FightProcessPSync.apply(
            "Having bent physics with your non-Euclidean curveball, you vanish.",
            prefs,
        )
        assertEquals(2, prefs.getInt("_curveballFightsLeft", -1))
    }

    @Test
    fun yearbookCameraPending() {
        FightProcessPSync.apply("Back to yearbook club", prefs)
        assertTrue(prefs.getBoolean("yearbookCameraPending", false))
    }

    @Test
    fun trainsetMoveIncrementsPosition() {
        prefs.setInt("trainsetPosition", 0)
        FightProcessPSync.apply(
            "Your toy train moves ahead to the coal hopper.",
            prefs,
            sessionLogger = logger,
        )
        assertEquals(1, prefs.getInt("trainsetPosition", -1))
    }

    @Test
    fun elfGratitudeOnWin() {
        FightProcessPSync.apply(
            "You grab a nearby elf and toss it.",
            prefs,
            won = true,
            sessionLogger = logger,
        )
        assertEquals(1, prefs.getInt("elfGratitude", 0))
    }

    @Test
    fun researchPointsMildProfessor() {
        FightProcessPSync.apply(
            "You jot down some notes quickly, before the fight starts.",
            prefs,
            mildManneredProfessor = true,
            sessionLogger = logger,
        )
        assertEquals(1, prefs.getInt("wereProfessorResearchPoints", 0))
    }

    @Test
    fun luckyGoldRingVolcoino() {
        FightProcessPSync.apply(
            "Your lucky gold ring gets warmer for a moment. You look down and find a Volcoino!",
            prefs,
            sessionLogger = logger,
        )
        assertTrue(prefs.getBoolean("_luckyGoldRingVolcoino", false))
    }

    @Test
    fun seadentConstructKill() {
        FightProcessPSync.apply(
            "tiny bits of their constituent construct parts are attracted to the magic of your spear. Whoa, they formed a whole new tine!",
            prefs,
            sessionLogger = logger,
        )
        assertEquals(1, prefs.getInt("seadentConstructKills", 0))
        assertEquals(1, prefs.getInt("seadentLevel", 0))
    }
}

class FightNodeSyncTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        FightCommentSync.reset()
        MonsterStatusTracker.resetLastMonster()
        runBlocking {
            MonsterDatabase.load()
            ItemDatabase.load()
        }
    }

    @Test
    fun cleeshTransformsOpponent() {
        MonsterStatusTracker.setNextMonster(testMonster(1, "old foe"), emptyList())
        assertTrue(
            FightNodeSync.applyCleesh(
                """<script>newpic("foo.gif","spooky vampire");</script>""",
                prefs,
                sessionLogger = null,
            ),
        )
        assertEquals("spooky vampire", prefs.getString(Preferences.LAST_MONSTER, ""))
    }

    @Test
    fun monsterIdTransformUpdatesTracker() {
        val first = MonsterDatabase.getById(1) ?: return
        val second = MonsterDatabase.getById(2) ?: return
        MonsterStatusTracker.setNextMonster(first, emptyList())
        FightCommentSync.lastMonsterId = first.id
        assertTrue(
            FightCommentSync.apply("<!--MONSTERID:${second.id}-->", prefs),
        )
        assertEquals(second.id, FightCommentSync.lastMonsterId)
        assertEquals(second.name, MonsterStatusTracker.getLastMonsterName())
    }

    @Test
    fun sameMonsterIdDoesNotRetransform() {
        val m = MonsterDatabase.getById(1) ?: return
        MonsterStatusTracker.setNextMonster(m, emptyList())
        FightCommentSync.lastMonsterId = m.id
        val before = MonsterStatusTracker.getLastMonsterName()
        FightCommentSync.apply("<!--MONSTERID:${m.id}-->", prefs)
        assertEquals(before, MonsterStatusTracker.getLastMonsterName())
    }

    @Test
    fun relItemGainsWithoutAcquireText() {
        val item = ItemDatabase.getById(1) ?: return
        val inv = CountingInventory()
        assertTrue(
            FightNodeSync.applyRelItems(
                html = """<table class="item" rel="id=${item.id}&n=1"></table>""",
                preferences = prefs,
                inventory = inv,
                character = null,
                effectManager = null,
            ),
        )
        assertEquals(1, inv.gained[item.id])
    }

    @Test
    fun relItemSkippedWhenAcquireTextPresent() {
        val item = ItemDatabase.getById(1) ?: return
        val inv = CountingInventory()
        assertFalse(
            FightNodeSync.applyRelItems(
                html = """You acquire an item: <b>${item.name}</b>
                    <table class="item" rel="id=${item.id}"></table>""",
                preferences = prefs,
                inventory = inv,
                character = null,
                effectManager = null,
            ),
        )
        assertEquals(null, inv.gained[item.id])
    }
}

private fun testMonster(id: Int, name: String) = MonsterDefinition(
    name = name,
    id = id,
    image = "test.gif",
    attack = 10,
    defense = 10,
    hp = 50,
    initiative = 50,
    meatDrop = 0,
    phylum = "beast",
    isBoss = false,
    isGhost = false,
    isLucky = false,
    isScaling = false,
    scale = 0,
    cap = 0,
    floor = 0,
    drops = emptyList(),
)

private class CountingInventory : InventoryManager(
    client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
    eventBus = GameEventBus(),
) {
    val gained = mutableMapOf<Int, Int>()
    override fun gainItemLocally(itemId: Int, quantity: Int) {
        gained[itemId] = (gained[itemId] ?: 0) + quantity
    }
}
