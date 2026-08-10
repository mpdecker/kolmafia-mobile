package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.quest.SkillGrantingEquipmentSync

class GameRuntimeLibraryAshP95Test {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    @Test
    fun revision_phase141() {
        assertEquals("phase400", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun have_skill_trueForCodpieceGemGrantedSkillNotInApi() {
        val codpiece = ItemData(
            id = SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID,
            name = SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ITEM,
            descId = "desc12067",
            image = "cod.gif",
            primaryUse = ItemPrimaryUse.ACCESSORY,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val gem = ItemData(
            id = 91001,
            name = "ash test gem",
            descId = "desc91001",
            image = "gem.gif",
            primaryUse = ItemPrimaryUse.ACCESSORY,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(codpiece)
        ItemDatabase.registerForTest(gem)
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 7419,
                name = "Drench Yourself in Sweat",
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
            gem.name,
            """Conditional Skill (Inventory): "Drench Yourself in Sweat"""",
        )
        val char = KoLCharacter()
        char.updateFromApiResponse(net.sourceforge.kolmafia.character.CharacterApiResponse(name = "Player"))
        char.updateEquipment(EquipmentSlot.CODPIECE1, gem.name)
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? =
                listOf(codpiece, gem).firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
        val inv = TestInventoryManager(
            mapOf(
                codpiece.id to InventoryItem(codpiece.id, codpiece.name, 1, ItemType.OTHER),
            ),
        )
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            skillManager = null,
            gameDatabase = db,
        )
        assertEquals(
            "true",
            outputLib(lib, """print(have_skill(to_skill("Drench Yourself in Sweat")));"""),
        )
    }
}
