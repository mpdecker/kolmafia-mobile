package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

class PeeVPeeSyncTest {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private fun character(
        fights: Int = 0,
        stoneBroken: Boolean = false,
        name: String = "Hero",
        mus: String = "10",
        mys: String = "8",
        mox: String = "7",
        musexp: String = "100",
        mysexp: String = "80",
        moxexp: String = "60",
    ): KoLCharacter = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(
                name = name,
                pvpfights = fights.toString(),
                hippystone = if (stoneBroken) "1" else "0",
                mus = mus,
                mys = mys,
                mox = mox,
                buffedmus = mus,
                buffedmys = mys,
                buffedmox = mox,
                musexp = musexp,
                mysexp = mysexp,
                moxexp = moxexp,
            ),
        )
    }

    private fun lossHtml(
        remaining: Int = 3,
        lostLine: String,
    ): String = """
        You have $remaining fights remaining today.
        <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
        <span class="win"><b>Villain</b> won the fight, <b>9</b> to <b>2</b>!
        <td>$lostLine</td>
    """.trimIndent()

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("", HttpStatusCode.OK) }), GameEventBus())

    private fun itemTableHtml(
        itemId: Int = 1,
        count: Int? = null,
        title: String = "seal-clubbing club",
    ): String {
        val n = if (count != null) "&n=$count" else ""
        return """<table class="item" rel="id=$itemId$n"><img title="$title" onclick="descitem(868780591)"></table>"""
    }

    private val fightPageHtml = """
        You have 7 fights remaining today.
        <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option></select>
    """.trimIndent()

    @Test
    fun fightPage_parsesAttacksStoneAndStances() {
        val char = character(fights = 0, stoneBroken = false)
        PeeVPeeSync.apply(
            html = fightPageHtml,
            url = "https://www.kingdomofloathing.com/peevpee.php?place=fight",
            character = char,
        )
        assertEquals(7, char.state.value.pvpFightsLeft)
        assertTrue(char.state.value.hippyStoneBroken)
        assertTrue(PvpManager.stancesKnown)
        assertEquals(1, PvpManager.findStance("Beary Famous"))
    }

    @Test
    fun fightPage_outOfFights_setsZeroAttacks() {
        val char = character(fights = 3, stoneBroken = true)
        PeeVPeeSync.apply(
            html = "You're out of fights!",
            url = "peevpee.php?place=fight",
            character = char,
        )
        assertEquals(0, char.state.value.pvpFightsLeft)
        assertTrue(char.state.value.hippyStoneBroken)
    }

    @Test
    fun fightPage_unbrokenStone_keepsFights() {
        val char = character(fights = 4, stoneBroken = true)
        PeeVPeeSync.apply(
            html = "Magical Mystical Hippy Stone",
            url = "peevpee.php?place=fight",
            character = char,
        )
        assertEquals(4, char.state.value.pvpFightsLeft)
        assertFalse(char.state.value.hippyStoneBroken)
    }

    @Test
    fun smashstone_shatter_setsTenAttacks() {
        val char = character(fights = 0, stoneBroken = false)
        PeeVPeeSync.apply(
            html = "You shatter your Magical Mystical Hippy Stone.",
            url = "peevpee.php?action=smashstone&confirm=on",
            character = char,
        )
        assertEquals(10, char.state.value.pvpFightsLeft)
        assertTrue(char.state.value.hippyStoneBroken)
    }

    @Test
    fun shopUrl_isNoOpForStancesAndAttacks() {
        val char = character(fights = 2, stoneBroken = true)
        PeeVPeeSync.apply(
            html = fightPageHtml,
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
            character = char,
        )
        assertEquals(2, char.state.value.pvpFightsLeft)
        assertFalse(PvpManager.stancesKnown)
    }

    @Test
    fun fightActionUrl_parsesAttacksOnFightPost() {
        val char = character(fights = 5, stoneBroken = true)
        PeeVPeeSync.apply(
            html = fightPageHtml,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(7, char.state.value.pvpFightsLeft)
        assertTrue(char.state.value.hippyStoneBroken)
        assertFalse(PvpManager.stancesKnown)
    }

    @Test
    fun fightResult_win_logsChallengeAndRecordsVictory() {
        val char = character(fights = 6, stoneBroken = true)
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val html = """
            You have 5 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Hero</b> won the fight, <b>12</b> to <b>5</b>!
            You gain a little swagger <b>(+3)</b>
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
            preferences = prefs,
            sessionLogger = logger,
        )
        assertEquals(5, char.state.value.pvpFightsLeft)
        assertEquals(3, prefs.getInt("availableSwagger", 0))
        assertEquals("Villain,", prefs.getString("currentPvpVictories", ""))
        assertTrue(logger.recentLines().any { it.contains("You challenged Villain and won the PvP fight, 12 to 5!") })
        assertFalse(PvpManager.noFight)
    }

    @Test
    fun fightResult_loss_doesNotRecordVictory() {
        val char = character(fights = 4, stoneBroken = true)
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val html = """
            You have 3 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Villain</b> won the fight, <b>9</b> to <b>2</b>!
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
            preferences = prefs,
            sessionLogger = logger,
        )
        assertEquals("", prefs.getString("currentPvpVictories", ""))
        assertTrue(logger.recentLines().any { it.contains("You challenged Villain and lost the PvP fight, 2 to 9!") })
    }

    @Test
    fun fightResult_compactWin() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val html = """
            You have 2 fights remaining today.
            <a href="showplayer.php?who=1">Hero</a> vs <a href="showplayer.php?who=2">Target</a>
            align="center"><b>Hero</b> Wins!</td>
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = character(fights = 3, stoneBroken = true),
            preferences = prefs,
            sessionLogger = logger,
        )
        assertEquals("Target,", prefs.getString("currentPvpVictories", ""))
        assertTrue(logger.recentLines().any { it.contains("You challenged Target and won the PvP fight") })
        assertFalse(logger.recentLines().any { it.contains(" to ") })
    }

    @Test
    fun fightResult_invalidTarget_doesNotAbort() {
        val char = character(fights = 6, stoneBroken = true)
        PeeVPeeSync.apply(
            html = """
                <tr><td>You can't attack a player against whom you've already won a fight today.
                You have 6 fights remaining today.
            """.trimIndent(),
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(6, char.state.value.pvpFightsLeft)
        assertFalse(PvpManager.noFight)
        assertEquals(null, PvpManager.abortReason)
    }

    @Test
    fun fightResult_clanPledge_setsAbort() {
        PeeVPeeSync.apply(
            html = "<tr><td><p>Before entering combat, you must pledge your allegiance to a clan for the season.",
            url = "peevpee.php?place=fight&action=fight",
            character = character(fights = 4, stoneBroken = true),
        )
        assertEquals("You need to pledge allegiance to a clan first.", PvpManager.abortReason)
    }

    @Test
    fun fightResult_unbrokenOpponentStone_setsNoFight() {
        PeeVPeeSync.apply(
            html = "Your opponent contains a Mystical Magical Hippy Stone. You have 4 fights remaining today.",
            url = "peevpee.php?place=fight&action=fight",
            character = character(fights = 4, stoneBroken = true),
        )
        assertTrue(PvpManager.noFight)
        assertEquals(null, PvpManager.abortReason)
    }

    @Test
    fun fightResult_loss_decrementsMuscleSubstats() {
        val char = character(fights = 4, stoneBroken = true)
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val html = """
            You have 3 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Villain</b> won the fight, <b>9</b> to <b>2</b>!
            <td>Hero lost 5 Muscle.</td>
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
            preferences = prefs,
            sessionLogger = logger,
        )
        assertEquals(95L, char.state.value.muscSubpoints)
        assertEquals(80L, char.state.value.mystSubpoints)
        assertEquals(60L, char.state.value.moxieSubpoints)
        assertEquals(9, char.state.value.baseMusc)
        assertEquals(10, char.state.value.buffedMusc)
        assertTrue(logger.recentLines().any { it.contains("Hero lost 5 Muscle") })
    }

    @Test
    fun fightResult_loss_doesNotDropBaseWhenStillAtSquare() {
        val char = character(fights = 4, stoneBroken = true, musexp = "105")
        assertEquals(10, char.state.value.baseMusc)
        PeeVPeeSync.apply(
            html = lossHtml(lostLine = "Hero lost 5 Muscle."),
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(100L, char.state.value.muscSubpoints)
        assertEquals(10, char.state.value.baseMusc)
        assertEquals(10, char.state.value.buffedMusc)
    }

    @Test
    fun fightResult_loss_dropsBaseWhenCrossingSquare() {
        val char = character(fights = 4, stoneBroken = true, musexp = "100")
        assertEquals(10, char.state.value.baseMusc)
        PeeVPeeSync.apply(
            html = lossHtml(lostLine = "Hero lost 1 Muscle."),
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(99L, char.state.value.muscSubpoints)
        assertEquals(9, char.state.value.baseMusc)
        assertEquals(10, char.state.value.buffedMusc)
    }

    @Test
    fun fightResult_loss_mapsFlavorStatsToCorrectPools() {
        val char = character(fights = 4, stoneBroken = true)
        val html = """
            You have 3 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Villain</b> won the fight, <b>9</b> to <b>2</b>!
            <td>Hero lost 3 Beefiness.</td>
            <td>Hero lost 4 Mysticality.</td>
            <td>Hero lost 2 Moxie.</td>
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(97L, char.state.value.muscSubpoints)
        assertEquals(76L, char.state.value.mystSubpoints)
        assertEquals(58L, char.state.value.moxieSubpoints)
        assertEquals(9, char.state.value.baseMusc)
        assertEquals(8, char.state.value.baseMyst)
        assertEquals(7, char.state.value.baseMoxie)
    }

    @Test
    fun fightResult_loss_wizardlinessDropsBaseMystWhenCrossingSquare() {
        val char = character(
            fights = 4,
            stoneBroken = true,
            mys = "9",
            mysexp = "81",
        )
        assertEquals(9, char.state.value.baseMyst)
        PeeVPeeSync.apply(
            html = lossHtml(lostLine = "Hero lost 4 Wizardliness."),
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(77L, char.state.value.mystSubpoints)
        assertEquals(8, char.state.value.baseMyst)
        assertEquals(9, char.state.value.buffedMyst)
    }

    @Test
    fun fightResult_loss_chutzpahDropsBaseMoxieWhenCrossingSquare() {
        val char = character(
            fights = 4,
            stoneBroken = true,
            mox = "7",
            moxexp = "49",
        )
        assertEquals(7, char.state.value.baseMoxie)
        PeeVPeeSync.apply(
            html = lossHtml(lostLine = "Hero lost 2 Chutzpah."),
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(47L, char.state.value.moxieSubpoints)
        assertEquals(6, char.state.value.baseMoxie)
        assertEquals(7, char.state.value.buffedMoxie)
    }

    @Test
    fun fightResult_compactLoss_skipsStatParse() {
        val char = character(fights = 3, stoneBroken = true)
        val html = """
            You have 2 fights remaining today.
            <a href="showplayer.php?who=1">Hero</a> vs <a href="showplayer.php?who=2">Target</a>
            align="center"><b>Target</b> Wins!</td>
            <td>Hero lost 5 Muscle.</td>
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(100L, char.state.value.muscSubpoints)
        assertEquals(10, char.state.value.baseMusc)
    }

    @Test
    fun fightResult_win_doesNotParseStatLoss() {
        val char = character(fights = 4, stoneBroken = true)
        val html = """
            You have 3 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Hero</b> won the fight, <b>12</b> to <b>5</b>!
            <td>Hero lost 5 Muscle.</td>
        """.trimIndent()
        PeeVPeeSync.apply(
            html = html,
            url = "peevpee.php?place=fight&action=fight",
            character = char,
        )
        assertEquals(100L, char.state.value.muscSubpoints)
        assertEquals(10, char.state.value.baseMusc)
    }

    @Test
    fun peevpeeVisit_withoutLid_gainsInventoryFromItemTable() {
        val inv = inventory()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        PeeVPeeSync.apply(
            html = itemTableHtml(itemId = 1, count = 2),
            url = "peevpee.php?place=fight&action=fight",
            character = character(fights = 4, stoneBroken = true),
            preferences = prefs,
            sessionLogger = logger,
            inventoryManager = inv,
        )
        assertEquals(2, inv.state.value.items[1]?.quantity)
        assertTrue(logger.recentLines().any { it.contains("You acquire an item:") })
    }

    @Test
    fun peevpeeVisit_withLid_skipsItemParse() {
        val inv = inventory()
        PeeVPeeSync.apply(
            html = itemTableHtml(itemId = 1, count = 2),
            url = "peevpee.php?place=fight&lid=3",
            character = character(fights = 4, stoneBroken = true),
            inventoryManager = inv,
        )
        assertTrue(inv.state.value.items.isEmpty())
    }

    @Test
    fun peevpeeShopVisit_withoutLid_gainsInventory() {
        val inv = inventory()
        val char = character(fights = 2, stoneBroken = true)
        PeeVPeeSync.apply(
            html = itemTableHtml(itemId = 1),
            url = "peevpee.php?place=shop",
            character = char,
            inventoryManager = inv,
        )
        assertEquals(1, inv.state.value.items[1]?.quantity)
        assertEquals(2, char.state.value.pvpFightsLeft)
        assertFalse(PvpManager.stancesKnown)
    }
}
