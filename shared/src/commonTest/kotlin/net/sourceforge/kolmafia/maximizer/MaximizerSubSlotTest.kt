package net.sourceforge.kolmafia.maximizer

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.shop.FolderHolderAccessibility

class MaximizerSubSlotTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun buildEquipmentMap_preservesFolderAndStickerSubSlots() {
        registerItem(FolderHolderAccessibility.FOLDER_HOLDER, "over-the-shoulder Folder Holder", ItemPrimaryUse.ACCESSORY)
        registerItem(6618, "folder (0)", ItemPrimaryUse.ACCESSORY)
        registerItem(3508, "Scratch 'n' Sniff Sword", ItemPrimaryUse.WEAPON)
        registerItem(9001, "test sticker", ItemPrimaryUse.ACCESSORY)

        val baseState = CharacterState(
            equipment = mapOf(
                EquipmentSlot.ACC1 to "over-the-shoulder Folder Holder",
                EquipmentSlot.FOLDER1 to "folder (0)",
                EquipmentSlot.WEAPON to "Scratch 'n' Sniff Sword",
                EquipmentSlot.STICKER1 to "test sticker",
            ),
        )
        val map = MaximizerSpeculation.buildEquipmentMap(baseState, emptyMap())
        assertEquals("folder (0)", map[EquipmentSlot.FOLDER1])
        assertEquals("test sticker", map[EquipmentSlot.STICKER1])
    }

    @Test
    fun folderHolderCandidateScoresWithLiveFolderModifiers() {
        registerItem(FolderHolderAccessibility.FOLDER_HOLDER, "over-the-shoulder Folder Holder", ItemPrimaryUse.ACCESSORY)
        registerItem(6618, "folder (0)", ItemPrimaryUse.ACCESSORY)
        ModifierDatabase.injectForTest("Item", "over-the-shoulder Folder Holder", "Mysticality: +1")
        ModifierDatabase.injectForTest("Item", "folder (0)", "Mysticality: +5")

        val baseState = CharacterState(
            challengePath = AscensionPath.KOLHS.apiName,
            equipment = mapOf(EquipmentSlot.FOLDER1 to "folder (0)"),
        )
        val assignment = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
        assignment[EquipmentSlot.ACC1] = "over-the-shoulder Folder Holder" to 0.0
        MaximizerSubSlotPreservation.applyParentPreservation(
            FolderHolderAccessibility.FOLDER_HOLDER,
            baseState,
            assignment,
        )

        val score = MaximizerSpeculation.scoreLoadout(
            baseState = baseState,
            assignment = assignment,
            evaluator = Evaluator("mys"),
            validateEquipment = false,
        )
        assertTrue(score >= 6.0, "expected folder + holder mys, got $score")
    }

    @Test
    fun stickerWeaponPreservesStickerSlotsInAssignment() {
        registerItem(3508, "Scratch 'n' Sniff Sword", ItemPrimaryUse.WEAPON)
        registerItem(9001, "test sticker", ItemPrimaryUse.ACCESSORY)
        ModifierDatabase.injectForTest("Item", "test sticker", "Meat Drop: +10")

        val baseState = CharacterState(
            equipment = mapOf(
                EquipmentSlot.STICKER1 to "test sticker",
            ),
        )
        val assignment = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
        assignment[EquipmentSlot.WEAPON] = "Scratch 'n' Sniff Sword" to 0.0
        MaximizerSubSlotPreservation.applyParentPreservation(3508, baseState, assignment)

        assertEquals("test sticker", assignment[EquipmentSlot.STICKER1]?.first)
    }

    @Test
    fun emitSubSlot_whenPlanDiffersFromLiveGear() {
        registerItem(6618, "folder (0)", ItemPrimaryUse.ACCESSORY)
        val charState = CharacterState(
            equipment = mapOf(EquipmentSlot.FOLDER1 to "old folder"),
        )
        val boosts = MaximizerEmitSlot.buildBoosts(
            MaximizerEmitSlot.Context(
                plan = MaximizerEmitSlot.Plan(
                    goal = "mys",
                    spec = MaximizeSpec(DoubleModifier.MYS, Evaluator("mys")),
                    scoreBefore = 0.0,
                    scoreAfter = 5.0,
                    bestPerSlot = mapOf(EquipmentSlot.FOLDER1 to ("folder (0)" to 5.0)),
                ),
                charState = charState,
                inventory = MaximizerEmitSlot.InventorySnapshot(),
                inventoryCount = { id -> if (id == 6618) 1 else 0 },
                gameDatabase = object : net.sourceforge.kolmafia.data.GameDatabase() {
                    override fun item(id: Int) = ItemDatabase.getById(id)
                    override fun item(name: String) = ItemDatabase.getByName(name)
                },
                preferences = null,
                mallPriceManager = null,
                priceLevel = MaximizerPriceLevel.DONT_CHECK,
            ),
        ).filter { it.slot == EquipmentSlot.FOLDER1 }

        assertEquals(1, boosts.size)
        assertTrue(boosts.single().text.contains("equip FOLDER1 folder (0)", ignoreCase = true))
    }

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
