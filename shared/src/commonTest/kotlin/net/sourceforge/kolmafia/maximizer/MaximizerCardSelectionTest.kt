package net.sourceforge.kolmafia.maximizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier

class MaximizerCardSelectionTest {

    @Test
    fun cardNeeded_trueWhenCardSleeveRanked() {
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.OFFHAND).add(
            MaximizerRankedItem(
                itemId = MaximizerCardSelection.CARD_SLEEVE_ID,
                name = MaximizerCardSelection.CARD_SLEEVE_NAME,
                score = 1.0,
                checked = MaximizerCheckedItem(
                    MaximizerCardSelection.CARD_SLEEVE_ID,
                    MaximizerCardSelection.CARD_SLEEVE_NAME,
                    initial = 1,
                ),
            ),
        )
        assertTrue(MaximizerCardSelection.cardNeeded(buckets))
    }

    @Test
    fun selectBestCard_picksHigherItemDropCard() {
        ModifierDatabase.injectForTest("Item", "Alice's Army Ninja", "Item Drop: +10")
        ModifierDatabase.injectForTest("Item", "Alice's Army Coward", "Item Drop: +1")
        ModifierDatabase.injectForTest("Item", "card sleeve", "Item Drop: +0")
        val db = stubDb()
        val buckets = SlotList<MaximizerRankedItem>()
        buckets.get(MaximizerSlot.OFFHAND).add(
            MaximizerRankedItem(
                itemId = MaximizerCardSelection.CARD_SLEEVE_ID,
                name = MaximizerCardSelection.CARD_SLEEVE_NAME,
                score = 1.0,
                checked = MaximizerCheckedItem(
                    MaximizerCardSelection.CARD_SLEEVE_ID,
                    MaximizerCardSelection.CARD_SLEEVE_NAME,
                    initial = 1,
                ),
            ),
        )
        val spec = MaximizeSpec(DoubleModifier.ITEMDROP)
        val best = MaximizerCardSelection.selectBestCard(
            spec = spec,
            charState = CharacterState(),
            rankedBuckets = buckets,
            countFor = { id ->
                when (id) {
                    4972 -> 1
                    4982 -> 1
                    else -> 0
                }
            },
            gameDatabase = db,
        )
        assertEquals("Alice's Army Ninja", best)
    }

    @Test
    fun cardForOffhand_nullWhenNotCardSleeve() {
        assertNull(
            MaximizerCardSelection.cardForOffhand("knife", "Alice's Army Ninja", CharacterState()),
        )
    }

    @Test
    fun cardForOffhand_returnsBestCardWhenOffhandIsSleeve() {
        assertEquals(
            "Alice's Army Ninja",
            MaximizerCardSelection.cardForOffhand(
                MaximizerCardSelection.CARD_SLEEVE_NAME,
                "Alice's Army Ninja",
                CharacterState(),
            ),
        )
    }

    private fun stubDb(): GameDatabase = object : GameDatabase() {
        private val items = mapOf(
            4972 to itemData(4972, "Alice's Army Ninja", ItemPrimaryUse.CARD),
            4982 to itemData(4982, "Alice's Army Coward", ItemPrimaryUse.CARD),
            5009 to itemData(5009, MaximizerCardSelection.CARD_SLEEVE_NAME, ItemPrimaryUse.OFFHAND),
        )
        override fun item(id: Int): ItemData? = items[id]
        override fun item(name: String): ItemData? =
            items.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
        override fun itemModifier(name: String) = ModifierDatabase.getItem(name)

        private fun itemData(id: Int, name: String, use: ItemPrimaryUse) =
            ItemData(id, name, "", "", use, emptySet(), setOf('t'), 0, null)
    }
}
