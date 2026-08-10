package net.sourceforge.kolmafia.maximizer

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier

class MaximizerSpeculationFoldDedupTest {

    @BeforeTest
    fun setup() {
        FoldGroupDatabase.resetForTest()
        ModifierDatabase.injectForTest("Item", "fold-a", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "fold-b", "Meat Drop: +2")
    }

    @AfterTest
    fun teardown() {
        FoldGroupDatabase.resetForTest()
    }

    @Test
    fun speculate_skipsFoldPeerAlreadyEquipped() {
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(hpDamagePct = 5, items = listOf("fold-a", "fold-b")),
        )
        val db = stubDb()
        val spec = MaximizeSpec(DoubleModifier.MEATDROP)
        val candidates = mapOf(
            EquipmentSlot.HAT to listOf("fold-a" to 1.0, "fold-b" to 2.0),
            EquipmentSlot.SHIRT to listOf("plain-shirt" to 0.5),
            EquipmentSlot.PANTS to listOf("plain-pants" to 0.5),
            EquipmentSlot.WEAPON to emptyList(),
            EquipmentSlot.OFFHAND to emptyList(),
            EquipmentSlot.CONTAINER to emptyList(),
            EquipmentSlot.ACC1 to emptyList(),
            EquipmentSlot.ACC2 to emptyList(),
            EquipmentSlot.ACC3 to emptyList(),
            EquipmentSlot.FAMILIAR to emptyList(),
        )
        val result = MaximizerSpeculation.speculate(
            spec = spec,
            baseState = CharacterState(),
            candidatesBySlot = candidates,
            budget = ComboBudget(50),
            seed = emptyMap(),
            bestModes = emptyMap(),
            gameDatabase = db,
            foldablesEnabled = true,
            countFor = { name ->
                when (name.lowercase()) {
                    "fold-a", "fold-b" -> 1
                    else -> 1
                }
            },
        )
        val hat = result[EquipmentSlot.HAT]?.first
        assertEquals("fold-b", hat)
    }

    private fun stubDb(): GameDatabase = object : GameDatabase() {
        override fun item(id: Int): ItemData? = null
        override fun item(name: String): ItemData? = ItemData(
            name.hashCode(),
            name,
            "",
            "",
            when (name.lowercase()) {
                "fold-a", "fold-b" -> ItemPrimaryUse.HAT
                "plain-shirt" -> ItemPrimaryUse.SHIRT
                "plain-pants" -> ItemPrimaryUse.PANTS
                else -> ItemPrimaryUse.ACCESSORY
            },
            emptySet(),
            setOf('t'),
            0,
            null,
        )
        override fun itemModifier(name: String) = ModifierDatabase.getItem(name)
    }
}
