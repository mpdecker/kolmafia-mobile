package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

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
            preferences = Preferences(MapSettings()).apply {
                setString("edPiece", "bear")
            },
        )
        assertEquals("fish", modes[Modeable.EDPIECE])
    }
}
