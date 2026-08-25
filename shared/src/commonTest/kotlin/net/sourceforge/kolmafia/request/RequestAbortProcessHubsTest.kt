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
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.skill.UseSkillSync

class RequestAbortProcessHubsTest {
    private lateinit var inventory: InventoryManager
    private lateinit var prefs: Preferences
    private lateinit var character: KoLCharacter

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
        RequestAbortGate.resetForTest()
        UseSkillSync.resetForTest()
        TransferItemSync.resetForTest()
        ChoiceCombatAshState.currentRound = 0
        ChoiceCombatAshState.inMultiFight = false
        ChoiceCombatAshState.handlingChoice = false
        ChoiceCombatAshState.choiceFollowsFight = false
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        inventory = InventoryManager(HttpClient(engine), GameEventBus())
        prefs = Preferences(MapSettings())
        character = KoLCharacter()
        character.updateMeat(1000)
        character.updateHpMp(50, 100, 80, 100)
        character.updateAdventuresLeft(40)
        character.updateFamiliar(1, "Familiar", 10, 0)
    }

    @Test
    fun abortGate_trueInFight() {
        ChoiceCombatAshState.currentRound = 2
        assertTrue(RequestAbortGate.abortIfInFightOrChoice())
        assertTrue(CreateAbortGate.shouldAbort())
    }

    @Test
    fun abortGate_trueInChoice() {
        ChoiceCombatAshState.handlingChoice = true
        assertTrue(RequestAbortGate.shouldAbort())
    }

    @Test
    fun abortGate_falseWhenIdle() {
        assertFalse(RequestAbortGate.shouldAbort())
    }

    @Test
    fun useItem_abortsWithoutHttpWhenInFight() = runBlocking {
        ChoiceCombatAshState.currentRound = 1
        var hits = 0
        val client = HttpClient(MockEngine {
            hits++
            respond("ok", HttpStatusCode.OK)
        })
        val req = UseItemRequest(client)
        val result = req.use(1, 1)
        assertTrue(result.isFailure)
        assertEquals(0, hits)
    }

    @Test
    fun useSkillSync_deductsMpOnSuccess() {
        UseSkillSync.noteCast(skillId = 3000, quantity = 2)
        val stopped = UseSkillSync.parseResponse(
            urlString = "skills.php?action=useskill&whichskill=3000&quantity=2",
            responseText = "You cast the skill.",
            preferences = prefs,
            character = character,
            mpCostPerCast = 10,
        )
        assertFalse(stopped)
        assertEquals(60, character.state.value.currentMp)
        assertEquals(3000, prefs.getInt("lastSkillUsed", -1))
        assertEquals(2, prefs.getInt("lastSkillCount", 0))
    }

    @Test
    fun useSkillSync_failureDoesNotSpendMp() {
        UseSkillSync.noteCast(skillId = 3000, quantity = 1)
        val stopped = UseSkillSync.parseResponse(
            urlString = "skills.php?action=useskill&whichskill=3000&quantity=1",
            responseText = "You don't have enough Mana Points.",
            preferences = prefs,
            character = character,
            mpCostPerCast = 10,
        )
        assertTrue(stopped)
        assertEquals(80, character.state.value.currentMp)
        assertTrue(UseSkillSync.lastUpdate.contains("Not enough", ignoreCase = true))
    }

    @Test
    fun transferCloset_putConsumesInventory() {
        inventory.gainItemLocally(1, 5)
        TransferItemSync.closetCounts = mutableMapOf()
        val ok = TransferItemSync.parseClosetTransfer(
            url = "closet.php?action=put&whichitem=1&qty=2",
            html = "You place stuff in your closet.",
            itemId = 1,
            quantity = 2,
            inventory = inventory,
            character = character,
        )
        assertTrue(ok)
        assertEquals(3, inventory.state.value.items[1]?.quantity)
        assertEquals(2, TransferItemSync.closetCounts!![1])
    }

    @Test
    fun transferStorage_pullGainsInventory() {
        TransferItemSync.storageCounts = mutableMapOf(42 to 4)
        prefs.setInt("pulls_remaining", 10)
        val ok = TransferItemSync.parseStorageTransfer(
            url = "storage.php?action=pullitem&whichitem=42&qty=3",
            html = "moved from storage to inventory",
            itemId = 42,
            quantity = 3,
            inventory = inventory,
            character = character,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(3, inventory.state.value.items[42]?.quantity)
        assertEquals(1, TransferItemSync.storageCounts!![42])
        assertEquals(7, prefs.getInt("pulls_remaining", -1))
    }

    @Test
    fun resultProcessor_processMeatAndStats() {
        ResultProcessor.processMeat(250, character)
        assertEquals(1250, character.state.value.meat)

        val before = character.state.value.muscSubpoints
        ResultProcessor.processStatGain("You gain 5 Muscle.", character)
        assertEquals(before + 5, character.state.value.muscSubpoints)

        ResultProcessor.processFamiliarWeightGain("Your familiar gains a pound!", character)
        assertEquals(11, character.state.value.familiarWeight)

        ResultProcessor.processAdventuresUsed(3, character)
        assertEquals(37, character.state.value.adventuresLeft)
    }
}
