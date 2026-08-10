package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerBoostCostSuffixTest {

    private val stubDb = object : GameDatabase() {
        override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
        override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun appendToText_includesEatFullnessSuffix() {
        registerFood(8101, "test food", fullness = 3)
        val costs = MaximizerBoostCostSuffix.accumulateFromCmd(
            "eat 1 \u00B68101",
            costContext(),
        )
        val text = MaximizerBoostCostSuffix.appendToText("eat test food (", costs) + "1)"
        assertTrue(text.contains("3 full"), text)
    }

    @Test
    fun applyCapacityGreyout_clearsCmdWhenStomachFull() {
        registerFood(8102, "heavy food", fullness = 5)
        val costs = MaximizerBoostCostSuffix.accumulateFromCmd(
            "eat 1 \u00B68102",
            costContext(),
        )
        val charState = CharacterState(fullness = 12, fullnessLimit = 15)
        val cmd = MaximizerBoostCostSuffix.applyCapacityGreyout("eat 1 \u00B68102", costs, charState)
        assertEquals("", cmd)
    }

    @Test
    fun shouldSkipBoost_whenMaximizerNoAdventuresAndAdvCost() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("maximizerNoAdventures", true)
        }
        val costs = MaximizerBoostCostSuffix.BoostCosts(adv = 2)
        assertTrue(MaximizerBoostCostSuffix.shouldSkipBoost(costs, prefs))
    }

    private fun costContext(): MaximizerBoostCostSuffix.Context =
        MaximizerBoostCostSuffix.Context(
            gameDatabase = stubDb,
            charState = CharacterState(),
        )

    private fun registerFood(id: Int, name: String, fullness: Int) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = ConsumableType.FOOD,
                amount = fullness,
                levelReq = 1,
                quality = ConsumableQuality.GOOD,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
    }
}
