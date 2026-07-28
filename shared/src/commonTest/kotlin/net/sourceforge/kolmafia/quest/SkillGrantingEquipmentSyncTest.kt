package net.sourceforge.kolmafia.quest

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class SkillGrantingEquipmentSyncTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    private fun stubDb(vararg items: ItemData): GameDatabase =
        object : GameDatabase() {
            override fun item(name: String): ItemData? =
                items.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

    private fun testItem(id: Int, name: String): ItemData {
        val item = ItemData(
            id = id,
            name = name,
            descId = "desc$id",
            image = "test.gif",
            primaryUse = ItemPrimaryUse.PANTS,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(item)
        return item
    }

    private fun testSkill(
        id: Int,
        name: String,
        isNonCombat: Boolean,
        isCombat: Boolean = !isNonCombat,
    ) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = id,
                name = name,
                image = "skill.gif",
                tags = if (isNonCombat) setOf("nc") else setOf("combat"),
                mpCost = 0,
                duration = 0,
                isPassive = false,
                isCombat = isCombat,
                isNonCombat = isNonCombat,
                isSong = false,
            ),
        )
    }

    @Test
    fun inventoryConditionalSkill_includedWhenItemInInventory() {
        testSkill(7414, "Sweat Out Some Booze", isNonCombat = true)
        val pants = testItem(10929, "designer sweatpants")
        ModifierDatabase.injectForTest(
            "Item",
            pants.name,
            """Conditional Skill (Inventory): "Sweat Out Some Booze"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(pants.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val granted = SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(pants))
        assertEquals(setOf("Sweat Out Some Booze"), granted)
    }

    @Test
    fun equippedCombatConditionalSkill_excluded() {
        testSkill(9001, "Iron Tricorn Headbutt", isNonCombat = false)
        val hat = testItem(9000, "iron tricorn hat")
        ModifierDatabase.injectForTest(
            "Item",
            hat.name,
            """Conditional Skill (Equipped): "Iron Tricorn Headbutt"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf(hat.name),
            activeEffectNames = emptySet(),
        )
        assertTrue(SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(hat)).isEmpty())
    }

    @Test
    fun equippedNonCombatConditionalSkill_includedWhenEquipped() {
        testSkill(7419, "Drench Yourself in Sweat", isNonCombat = true)
        testSkill(7415, "Sweat Flick", isNonCombat = false)
        val pants = testItem(10929, "designer sweatpants")
        ModifierDatabase.injectForTest(
            "Item",
            pants.name,
            """Conditional Skill (Equipped): "Sweat Flick", Conditional Skill (Equipped): "Drench Yourself in Sweat"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf(pants.name),
            activeEffectNames = emptySet(),
        )
        val granted = SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(pants))
        assertEquals(setOf("Drench Yourself in Sweat"), granted)
        assertFalse(granted.contains("Sweat Flick"))
    }

    @Test
    fun filterItemId_limitsToSingleProvider() {
        testSkill(7414, "Sweat Out Some Booze", isNonCombat = true)
        testSkill(8001, "Other Inventory Skill", isNonCombat = true)
        val pants = testItem(10929, "designer sweatpants")
        val other = testItem(8000, "other provider")
        ModifierDatabase.injectForTest(
            "Item",
            pants.name,
            """Conditional Skill (Inventory): "Sweat Out Some Booze"""",
        )
        ModifierDatabase.injectForTest(
            "Item",
            other.name,
            """Conditional Skill (Inventory): "Other Inventory Skill"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(pants.id, other.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val granted = SkillGrantingEquipmentSync.grantedSkillNames(
            context,
            stubDb(pants, other),
            filterItemId = pants.id,
        )
        assertEquals(setOf("Sweat Out Some Booze"), granted)
    }

    @Test
    fun currentModifiersExpressionContext_seesEquipmentGrantedSkill() {
        testSkill(7414, "Sweat Out Some Booze", isNonCombat = true)
        val pants = testItem(10929, "designer sweatpants")
        ModifierDatabase.injectForTest(
            "Item",
            pants.name,
            """Conditional Skill (Inventory): "Sweat Out Some Booze"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(pants.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val granted = SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(pants))
        assertEquals(setOf("Sweat Out Some Booze"), granted)
        val expr = ExpressionContext.from(CharacterState(), emptyList(), granted)
        assertTrue(expr.hasSkill("Sweat Out Some Booze"))
    }

    @Test
    fun codpieceGem_inventoryConditionalSkill_includedWhenCodpieceAccessible() {
        testSkill(7419, "Drench Yourself in Sweat", isNonCombat = true)
        val codpiece = testItem(
            SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID,
            SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ITEM,
        )
        val gem = testItem(90001, "test codpiece gem")
        ModifierDatabase.injectForTest(
            "EternityCodpiece",
            gem.name,
            """Conditional Skill (Inventory): "Drench Yourself in Sweat"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(codpiece.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            codpieceGemNames = setOf(gem.name),
        )
        val granted = SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(codpiece, gem))
        assertEquals(setOf("Drench Yourself in Sweat"), granted)
    }

    @Test
    fun codpieceGem_equippedCombatSkill_excluded() {
        testSkill(9002, "Combat Gem Skill", isNonCombat = false)
        val codpiece = testItem(
            SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID,
            SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ITEM,
        )
        val gem = testItem(90002, "combat gem")
        ModifierDatabase.injectForTest(
            "EternityCodpiece",
            gem.name,
            """Conditional Skill (Equipped): "Combat Gem Skill"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(codpiece.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            codpieceGemNames = setOf(gem.name),
        )
        assertTrue(
            SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(codpiece, gem)).isEmpty(),
        )
    }

    @Test
    fun codpieceGem_skippedWhenCodpieceNotAccessible() {
        testSkill(7419, "Drench Yourself in Sweat", isNonCombat = true)
        val gem = testItem(90003, "orphan gem")
        ModifierDatabase.injectForTest(
            "EternityCodpiece",
            gem.name,
            """Conditional Skill (Inventory): "Drench Yourself in Sweat"""",
        )
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            codpieceGemNames = setOf(gem.name),
        )
        assertTrue(SkillGrantingEquipmentSync.grantedSkillNames(context, stubDb(gem)).isEmpty())
    }
}
