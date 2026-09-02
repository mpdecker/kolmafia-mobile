package net.sourceforge.kolmafia.skill

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.quest.SkillGrantingEquipmentSync
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.RequestAbortGate
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.EquipmentManager

class CodpieceSkillEquipTest {

    private val skillId = 7419
    private val skillName = "Drench Yourself in Sweat"
    private val gemId = 91001
    private val gemName = "ash test gem"

    @BeforeTest
    fun setUp() {
        UseSkillOptimize.resetForTest()
        UseSkillSync.resetForTest()
        RequestAbortGate.resetForTest()
        ChoiceCombatAshState.reset()
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        registerFixtures()
    }

    @AfterTest
    fun tearDown() {
        UseSkillOptimize.resetForTest()
        UseSkillSync.resetForTest()
        RequestAbortGate.resetForTest()
        ChoiceCombatAshState.reset()
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun optimizeEquipment_prefersCodpieceOverGemForSocketedNoncombatSkill() = runTest {
        val env = equippedGemEnvironment(codpieceInInventory = true)

        val prepared = UseSkillOptimize.optimizeEquipment(
            skillId = skillId,
            preferences = null,
            character = env.character,
            inventory = env.inventory,
            equipmentManager = env.equipmentManager,
            equipmentRequest = env.equipmentRequest,
            equip = false,
        )

        assertEquals(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID, prepared)
        assertEquals(
            SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID,
            UseSkillOptimize.lastPreparedToolId,
        )
        assertEquals(EquipmentSlot.PANTS, UseSkillOptimize.lastPreparedSlot)
    }

    @Test
    fun optimizeEquipment_doesNotPreferCodpieceForCombatSkill() = runTest {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 9002,
                name = "Combat Gem Skill",
                image = "skill.gif",
                tags = setOf("combat"),
                mpCost = 0,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
        ModifierDatabase.injectForTest(
            "EternityCodpiece",
            gemName,
            """Conditional Skill (Equipped): "Combat Gem Skill"""",
        )
        val env = equippedGemEnvironment(codpieceInInventory = true)

        val prepared = UseSkillOptimize.optimizeEquipment(
            skillId = 9002,
            preferences = null,
            character = env.character,
            inventory = env.inventory,
            equipmentManager = env.equipmentManager,
            equipmentRequest = env.equipmentRequest,
            equip = false,
        )

        assertEquals(-1, prepared)
        assertEquals(-1, UseSkillOptimize.lastPreparedToolId)
    }

    @Test
    fun optimizeEquipment_fallsBackWhenCodpieceUnavailable() = runTest {
        val env = equippedGemEnvironment(codpieceInInventory = false)

        val prepared = UseSkillOptimize.optimizeEquipment(
            skillId = skillId,
            preferences = null,
            character = env.character,
            inventory = env.inventory,
            equipmentManager = env.equipmentManager,
            equipmentRequest = env.equipmentRequest,
            equip = false,
        )

        assertEquals(-1, prepared)
        assertEquals(-1, UseSkillOptimize.lastPreparedToolId)
    }

    @Test
    fun optimizeEquipment_equipsPantsSlotWithoutUnsocketingGems() = runTest {
        val env = equippedGemEnvironment(codpieceInInventory = true)

        val prepared = UseSkillOptimize.optimizeEquipment(
            skillId = skillId,
            preferences = null,
            character = env.character,
            inventory = env.inventory,
            equipmentManager = env.equipmentManager,
            equipmentRequest = env.equipmentRequest,
            equip = true,
        )

        assertEquals(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID, prepared)
        assertEquals(
            listOf(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID to EquipmentSlot.PANTS),
            env.equipmentRequest.equippedItems,
        )
        assertEquals(0, env.equipmentRequest.unsocketCalls)
        assertEquals(gemName, env.character.state.value.equipment[EquipmentSlot.CODPIECE1])
    }

    @Test
    fun skillCastRequest_recordsCodpieceBeforeSkillsPhp() = runTest {
        val env = equippedGemEnvironment(codpieceInInventory = true)
        val paths = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                paths += request.url.encodedPath
                respond("You acquire an effect.", HttpStatusCode.OK)
            },
        )
        val castRequest = SkillCastRequest(
            client = client,
            character = env.character,
            inventoryManager = env.inventory,
            equipmentManager = env.equipmentManager,
            equipmentRequest = env.equipmentRequest,
        )

        val result = castRequest.cast(skillId, 1)

        assertTrue(result.isSuccess)
        assertEquals(
            SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID,
            UseSkillOptimize.lastPreparedToolId,
        )
        assertEquals(EquipmentSlot.PANTS, UseSkillOptimize.lastPreparedSlot)
        assertTrue(paths.any { it.contains("skills.php") })
        assertEquals(0, env.equipmentRequest.unsocketCalls)
        assertEquals(
            listOf(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID to EquipmentSlot.PANTS),
            env.equipmentRequest.equippedItems,
        )
    }

    private fun registerFixtures() {
        ItemDatabase.registerForTest(
            ItemData(
                id = SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID,
                name = SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ITEM,
                descId = "desc12067",
                image = "eternitycod.gif",
                primaryUse = ItemPrimaryUse.PANTS,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = gemId,
                name = gemName,
                descId = "desc$gemId",
                image = "gem.gif",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = skillId,
                name = skillName,
                image = "skill.gif",
                tags = setOf("nc"),
                mpCost = 0,
                duration = 0,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        ModifierDatabase.injectForTest(
            "EternityCodpiece",
            gemName,
            """Conditional Skill (Equipped): "$skillName"""",
        )
    }

    private fun equippedGemEnvironment(codpieceInInventory: Boolean): TestEnv {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.CODPIECE1, gemName)
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val inventory = InventoryManager(client, GameEventBus())
        if (codpieceInInventory) {
            inventory.gainItemLocally(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID, 1)
        }
        val equipmentManager = EquipmentManager(character, inventory)
        val equipmentRequest = RecordingEquipmentRequest(client)
        return TestEnv(character, inventory, equipmentManager, equipmentRequest)
    }

    private data class TestEnv(
        val character: KoLCharacter,
        val inventory: InventoryManager,
        val equipmentManager: EquipmentManager,
        val equipmentRequest: RecordingEquipmentRequest,
    )

    private class RecordingEquipmentRequest(
        client: HttpClient,
    ) : EquipmentRequest(client) {
        val equippedItems = mutableListOf<Pair<Int, EquipmentSlot>>()
        var unsocketCalls = 0

        override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
            if (slot in EquipmentSlot.CODPIECE_SLOTS) {
                unsocketCalls++
                return Result.success(Unit)
            }
            equippedItems += itemId to slot
            return Result.success(Unit)
        }

        override suspend fun equipCodpieceGem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
            unsocketCalls++
            return Result.success(Unit)
        }

        override suspend fun unequipCodpieceSlot(slot: EquipmentSlot): Result<Unit> {
            unsocketCalls++
            return Result.success(Unit)
        }
    }
}
