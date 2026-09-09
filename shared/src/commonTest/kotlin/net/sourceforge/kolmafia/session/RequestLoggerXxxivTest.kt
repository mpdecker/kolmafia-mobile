package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.FightRequestHub
import net.sourceforge.kolmafia.request.FriarRequestHub
import net.sourceforge.kolmafia.request.SafetyShelterRequest
import net.sourceforge.kolmafia.request.SendGiftRequestHub
import net.sourceforge.kolmafia.request.SendMailRequestHub
import net.sourceforge.kolmafia.request.SummoningChamberRequestHub

/**
 * Behavioral Deepen XXXIV Groups D/E — gift/mail/friar/summon/shelter/fight hubs.
 */
class RequestLoggerXxxivTest {
    private lateinit var prefs: Preferences
    private lateinit var logger: SessionLogger

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        logger = SessionLogger(prefs, GameEventBus())
        RequestLogger.currentRound = { 0 }
        RequestLogger.itemNameById = { id -> "item$id" }
        RequestLogger.fightActorName = { "Hero" }
        ChoiceCombatAshState.reset()
    }

    @AfterTest
    fun tearDown() {
        RequestLogger.itemNameById = { id -> ItemDatabase.getItemName(id).ifBlank { null } }
        RequestLogger.fightActorName = { "Player" }
        RequestLogger.currentRound = { ChoiceCombatAshState.currentRound }
        ChoiceCombatAshState.reset()
    }

    private fun lines(): List<String> = logger.recentLines().filter { it.isNotBlank() }

    // ── Group D: gift / kmail ───────────────────────────────────────────────

    @Test
    fun sendGift_logsRecipientAndItems() {
        val ok = SendGiftRequestHub.registerRequest(
            url = "town_sendgift.php",
            sessionLogger = logger,
            formFields = mapOf(
                "action" to "send",
                "towho" to "Buddy",
                "whichitem1" to "100",
                "howmany1" to "2",
            ),
        )
        assertTrue(ok)
        val log = lines().last()
        assertTrue(log.contains("send a gift to Buddy"))
        assertTrue(log.contains("2 item100"))
    }

    @Test
    fun sendMail_logsMeatAndItems() {
        val ok = SendMailRequestHub.registerRequest(
            url = "sendmessage.php?action=send&towho=Pal&whichitem=50&howmany=1&sendmeat=25",
            sessionLogger = logger,
        )
        assertTrue(ok)
        val log = lines().last()
        assertTrue(log.contains("send a kmail to Pal"))
        assertTrue(log.contains("item50"))
        assertTrue(log.contains("25 Meat"))
    }

    @Test
    fun requestLogger_sendGift_routesThroughHub() {
        assertTrue(
            RequestLogger.registerRequest(
                "town_sendgift.php?towho=Willow&whichitem1=42&howmany1=1",
                logger,
                prefs,
            ),
        )
        assertTrue(lines().any { it.contains("send a gift to Willow") })
    }

    @Test
    fun sendGift_rejectsPlainSendmessageWithoutGiftFlag() {
        assertFalse(
            SendGiftRequestHub.registerRequest("sendmessage.php?towho=x&action=send", logger),
        )
    }

    // ── Group D: friars / summon / shelter ──────────────────────────────────

    @Test
    fun friars_blessing_logsBro() {
        assertTrue(FriarRequestHub.registerRequest("friars.php?action=buffs&bro=2", logger))
        assertEquals("friars blessing 2", lines().last())
    }

    @Test
    fun friars_visit_claimsWithoutLog() {
        val before = lines().size
        assertTrue(FriarRequestHub.registerRequest("friars.php", logger))
        assertEquals(before, lines().size)
    }

    @Test
    fun summoningChamber_logsDemonName() {
        assertTrue(
            SummoningChamberRequestHub.registerRequest(
                "choice.php?whichchoice=922&option=1&demonname=Pikachu",
                logger,
            ),
        )
        assertEquals("summon Pikachu", lines().last())
    }

    @Test
    fun summoningChamber_formFields_demon() {
        assertTrue(
            SummoningChamberRequestHub.registerRequest(
                "choice.php",
                logger,
                formFields = mapOf(
                    "whichchoice" to "922",
                    "option" to "1",
                    "demonname" to "Neil",
                ),
            ),
        )
        assertEquals("summon Neil", lines().last())
    }

    @Test
    fun safetyShelter_vault3_spa() {
        assertTrue(
            SafetyShelterRequest.registerRequest(
                "place.php?whichplace=falloutshelter&action=vault3",
                logger,
            ),
        )
        assertEquals("Visiting your Spa Simulation Chamber", lines().last())
    }

    @Test
    fun safetyShelter_bareVisit_noLog() {
        val before = lines().size
        assertTrue(
            SafetyShelterRequest.registerRequest(
                "place.php?whichplace=falloutshelter",
                logger,
            ),
        )
        assertEquals(before, lines().size)
    }

    // ── Group E: fight registerRequest / After Battle ───────────────────────

    @Test
    fun fightAttack_logsRoundAction() {
        ChoiceCombatAshState.currentRound = 3
        val ok = FightRequestHub.registerRequest(
            url = "fight.php",
            sessionLogger = logger,
            preferences = prefs,
            formFields = mapOf("action" to "attack"),
        )
        assertTrue(ok)
        assertTrue(lines().any { it == "Round 3: Hero attacks!" })
        ChoiceCombatAshState.currentRound = 0
    }

    @Test
    fun fightUseItem_resolvesItemName() {
        ChoiceCombatAshState.currentRound = 1
        val ok = FightRequestHub.registerRequest(
            url = "fight.php",
            sessionLogger = logger,
            preferences = prefs,
            formFields = mapOf("action" to "useitem", "whichitem" to "777"),
        )
        assertTrue(ok)
        assertTrue(lines().any { it.contains("uses the item777") })
        ChoiceCombatAshState.currentRound = 0
    }

    @Test
    fun fightHub_bareFight_claimsWithoutActionLog() {
        val before = lines().size
        assertTrue(FightRequestHub.registerRequest("fight.php", logger, prefs))
        assertEquals(before, lines().size)
    }

    @Test
    fun fightHub_logBattleActionOff_skips() {
        prefs.setBoolean("logBattleAction", false)
        ChoiceCombatAshState.currentRound = 2
        val before = lines().size
        assertTrue(
            FightRequestHub.registerRequest(
                "fight.php",
                logger,
                prefs,
                formFields = mapOf("action" to "attack"),
            ),
        )
        assertEquals(before, lines().size)
    }

    @Test
    fun fightSessionLog_apply_winAfterBattle() {
        FightSessionLog.apply(
            html = "<p>You gain 3 Mysticality.</p>",
            sessionLogger = logger,
            won = true,
            fightEnded = true,
            monsterName = "spooky bat",
        )
        val out = lines()
        assertTrue(out.any { it.startsWith("After Battle: You gain 3 Mysticality") })
        assertTrue(out.any { it.contains("spooky bat wins the fight!") })
    }

    @Test
    fun parseTransferItems_prefersFormFields() {
        val items = RequestLogger.parseTransferItems(
            "storage.php",
            mapOf("whichitem1" to "10", "howmany1" to "3"),
        )
        assertEquals(listOf(10 to 3), items)
    }
}
