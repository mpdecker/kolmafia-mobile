package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.modifiers.DoubleModifier

class MaximizerModeSelectionTest {

    @BeforeTest
    fun loadModifiers() {
        runBlocking { ModifierDatabase.load() }
    }

    @Test
    fun itemGoal_picksBucketStyleForUmbrella() {
        val spec = MaximizeSpec(DoubleModifier.ITEMDROP)
        val charState = CharacterState()
        val ranked = SlotList<MaximizerRankedItem>()
        ranked.get(MaximizerSlot.OFFHAND).add(
            MaximizerRankedItem(
                itemId = 10899,
                name = "unbreakable umbrella",
                score = 1.0,
                checked = MaximizerCheckedItem(10899, "unbreakable umbrella", initial = 1),
            ),
        )
        val bestPerSlot = emptyMap<EquipmentSlot, Pair<String, Double>>()
        val modes = MaximizerModeSelection.selectBestModes(
            spec = spec,
            charState = charState,
            rankedBuckets = ranked,
            bestPerSlot = bestPerSlot,
            preferences = null,
        )
        assertEquals("bucket style", modes[Modeable.UMBRELLA])
    }

    @Test
    fun shieldGoal_forcesForwardFacingUmbrella() {
        val spec = MaximizeGoal.parseSpec("+shield, item") ?: error("parse failed")
        assertEquals("forward-facing", spec.forcedModeables[Modeable.UMBRELLA])
        assertTrue(spec.requireHands)
        val charState = CharacterState()
        val ranked = SlotList<MaximizerRankedItem>()
        ranked.get(MaximizerSlot.OFFHAND).add(
            MaximizerRankedItem(
                itemId = 10899,
                name = "unbreakable umbrella",
                score = 1.0,
                checked = MaximizerCheckedItem(10899, "unbreakable umbrella", initial = 1),
            ),
        )
        val modes = MaximizerModeSelection.selectBestModes(
            spec = spec,
            charState = charState,
            rankedBuckets = ranked,
            bestPerSlot = emptyMap(),
            preferences = null,
        )
        assertEquals("forward-facing", modes[Modeable.UMBRELLA])
    }

    @Test
    fun seaGoal_forcesFishEdpiece() {
        val spec = MaximizeGoal.parseSpec("sea, muscle") ?: error("parse failed")
        assertEquals("fish", spec.forcedModeables[Modeable.EDPIECE])
        val charState = CharacterState()
        val ranked = SlotList<MaximizerRankedItem>()
        ranked.get(MaximizerSlot.HAT).add(
            MaximizerRankedItem(
                itemId = 8185,
                name = "The Crown of Ed the Undying",
                score = 1.0,
                checked = MaximizerCheckedItem(8185, "The Crown of Ed the Undying", initial = 1),
            ),
        )
        val modes = MaximizerModeSelection.selectBestModes(
            spec = spec,
            charState = charState,
            rankedBuckets = ranked,
            bestPerSlot = emptyMap(),
            preferences = null,
        )
        assertEquals("fish", modes[Modeable.EDPIECE])
    }

    @Test
    fun backupSlots_includeFamiliarForOffhandModeableWhenCarryFamiliarPresent() {
        val slots = MaximizerModeSelection.modeableBackupSlots(
            Modeable.UMBRELLA,
            carryFamiliars = listOf(FamiliarCarryRules.HAND_RACE),
            gameDatabase = stubDb(),
        )
        assertTrue(EquipmentSlot.OFFHAND in slots)
        assertTrue(EquipmentSlot.FAMILIAR in slots)
    }

    @Test
    fun assignmentModeOverrides_appliesOnlyWhenModeableEquipped() {
        val bestModes = mapOf(Modeable.UMBRELLA to "bucket style")
        val without = MaximizerModeSelection.assignmentModeOverrides(
            mapOf(EquipmentSlot.WEAPON to ("knife" to 1.0)),
            bestModes,
        )
        assertTrue(without.isEmpty())

        val withUmbrella = MaximizerModeSelection.assignmentModeOverrides(
            mapOf(EquipmentSlot.FAMILIAR to ("unbreakable umbrella" to 1.0)),
            bestModes,
            carryFamiliars = listOf(FamiliarCarryRules.HAND_RACE),
            gameDatabase = stubDb(),
        )
        assertEquals("bucket style", withUmbrella[Modeable.UMBRELLA])
    }

    @Test
    fun backupCamera_spansAccessorySlots() {
        val slots = MaximizerModeSelection.modeableBackupSlots(Modeable.BACKUPCAMERA)
        assertEquals(
            setOf(EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3),
            slots,
        )
    }

    private fun stubDb(): GameDatabase = object : GameDatabase() {
        override fun item(id: Int): ItemData? = when (id) {
            10899 -> ItemData(
                10899,
                "unbreakable umbrella",
                "",
                "",
                ItemPrimaryUse.OFFHAND,
                emptySet(),
                setOf('t'),
                0,
                null,
            )
            else -> null
        }
        override fun item(name: String): ItemData? = item(10899)?.takeIf {
            it.name.equals(name, ignoreCase = true)
        }
    }
}
