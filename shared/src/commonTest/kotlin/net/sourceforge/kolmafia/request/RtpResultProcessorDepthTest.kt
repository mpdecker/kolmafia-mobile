package net.sourceforge.kolmafia.request

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
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.InventoryActionSync
import net.sourceforge.kolmafia.session.ResponseTextParser
import net.sourceforge.kolmafia.session.ResultProcessor

class RtpResultProcessorDepthTest {
    private lateinit var inventory: InventoryManager
    private lateinit var prefs: Preferences
    private lateinit var character: KoLCharacter

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
        BasementSync.resetForTest()
        ClanStashSync.resetForTest()
        TransferItemSync.resetForTest()
        TransferItemSync.closetCounts = mutableMapOf()
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        inventory = InventoryManager(HttpClient(engine), GameEventBus())
        prefs = Preferences(MapSettings())
        character = KoLCharacter()
        character.updateMeat(500)
        character.updateHpMp(40, 100, 30, 100)
        character.updateAdventuresLeft(20)
    }

    @Test
    fun accountSync_setsCombatPrefs() {
        val html = """
            <input checked="checked"  name="flag_wowbar" />
            <input checked="checked"  name="flag_bothcombatinterf" />
            <select name="autoattack"><option value="0"><option selected value="1">attack</option></select>
        """.trimIndent()
        AccountSync.parseAccountData("account.php?tab=combat", html, prefs, character)
        assertTrue(prefs.getBoolean("serverAddsCustomCombat", false))
        assertTrue(prefs.getBoolean("serverAddsBothCombat", false))
        assertEquals(1, character.state.value.autoAttackAction)
    }

    @Test
    fun charSheetSync_parsesHpMeatAdv() {
        val html = """
            PlayerName (#12345)
            Hit Points: 55 / 120
            Mana Points: 22 / 80
            Meat: 9,999
            Adventures Left: 33
            Sign: Packrat
            Class: Seal Clubber
        """.trimIndent()
        assertTrue(CharSheetSync.parseStatus(html, character, prefs))
        assertEquals(12345, character.state.value.playerId)
        assertEquals(55, character.state.value.currentHp)
        assertEquals(9999, character.state.value.meat)
        assertEquals(33, character.state.value.adventuresLeft)
        assertEquals("Packrat", character.state.value.zodiacSign)
    }

    @Test
    fun basementSync_setsLevelAndExpression() {
        val level = BasementSync.checkBasement("You are on Level 42 of Fernswarthy's Basement", prefs)
        assertEquals(42, level)
        assertEquals(42, prefs.getInt("basementLevel", 0))
        val ctx = ExpressionContext(basementLevel = prefs.getInt("basementLevel", 0))
        assertEquals(42.0, ctx.multiLetterVariable("BL"))
    }

    @Test
    fun autosellSync_consumesItemAndAddsMeat() {
        inventory.gainItemLocally(1, 5)
        val price = ItemDatabase.getById(1)?.autosellPrice ?: 0
        AutosellSync.parseCompact(
            "sellstuff.php?action=sell&whichitem=1&howmany=2",
            inventory,
            character,
        )
        assertEquals(3, inventory.state.value.items[1]?.quantity)
        assertEquals(500 + price * 2, character.state.value.meat)
    }

    @Test
    fun inventoryAction_closetPush() {
        inventory.gainItemLocally(2, 4)
        assertTrue(
            InventoryActionSync.parse(
                "inventory.php?action=closetpush&whichitem=2&qty=1",
                "ok",
                inventory,
                character,
                prefs,
            ),
        )
        assertEquals(3, inventory.state.value.items[2]?.quantity)
        assertEquals(1, TransferItemSync.closetCounts!![2])
    }

    @Test
    fun resultProcessor_gainLossHpAndDiscard() {
        ResultProcessor.processGainLoss("You gain 10 hit points.", character)
        assertEquals(50, character.state.value.currentHp)
        ResultProcessor.processGainLoss("You lose 5 Meat.", character)
        assertEquals(495, character.state.value.meat)

        inventory.gainItemLocally(3, 2)
        val name = ItemDatabase.getById(3)?.name ?: return
        ResultProcessor.processDiscard("You discard your $name.", inventory)
        assertEquals(1, inventory.state.value.items[3]?.quantity)
    }

    @Test
    fun responseTextParser_routesAccountAndBasement() {
        val pages = mutableListOf<String>()
        ResponseTextParser.externalUpdate(
            url = "account.php?tab=combat",
            html = """<input checked="checked"  name="flag_wowbar" />""",
            onRoute = { pages += it },
            preferences = prefs,
            character = character,
        )
        assertTrue(pages.contains("account"))
        assertTrue(prefs.getBoolean("serverAddsCustomCombat", false))

        ResponseTextParser.externalUpdate(
            url = "basement.php",
            html = "Level 7",
            onRoute = { pages += it },
            preferences = prefs,
        )
        assertTrue(pages.contains("basement"))
        assertEquals(7, BasementSync.basementLevel)
    }

    @Test
    fun arenaAndBounty_visitPrefs() {
        assertTrue(
            CakeArenaSync.parseResponse("arena.php", "You have 3 fights left", prefs, character),
        )
        assertEquals(3, prefs.getInt("cakeArenaFightsLeft", 0))
        assertTrue(
            BountyHunterSync.parseResponse(
                "bounty.php",
                "Current Bounty: <b>foo</b>",
                prefs,
                character,
            ),
        )
        assertEquals("foo", prefs.getString("currentBountyItem", ""))
    }
}
