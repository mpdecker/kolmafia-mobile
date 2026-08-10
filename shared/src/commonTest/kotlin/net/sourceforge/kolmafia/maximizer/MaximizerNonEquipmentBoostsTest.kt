package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData as StaticEffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerNonEquipmentBoostsTest {

    private val stubDb = object : GameDatabase() {
        override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
        override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        EffectDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun build_emitsHorseryBoostWhenAvailable() {
        ModifierDatabase.injectForTest("Horsery", "normal horse", "Initiative: +10")
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("horseryAvailable", true)
            setString("_horsery", "")
        }
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.INITIATIVE, Evaluator("init")),
                preferences = prefs,
            ),
        )
        assertTrue(
            boosts.any { !it.isEquipment && it.text.contains("horsery normal horse", ignoreCase = true) },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_effectGainIncludesFullnessSuffix() {
        registerFood(8201, "test food", fullness = 2)
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 82001,
                name = "Test Muscle Buff",
                image = "buff.gif",
                descId = "d82001",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "eat 1 test food",
            ),
        )
        ModifierDatabase.injectForTest("Effect", "Test Muscle Buff", "Muscle: +100")
        val prefs = Preferences(MapSettings())
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.MUS, Evaluator("mus")),
                preferences = prefs,
                inventoryCount = { id -> if (id == 8201) 1 else 0 },
            ),
        )
        assertTrue(
            boosts.any {
                !it.isEquipment &&
                    it.text.contains("eat 1 test food", ignoreCase = true) &&
                    it.text.contains("2 full")
            },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_emitsBoomBoxBoostWhenAvailable() {
        ModifierDatabase.injectForTest("BoomBox", "Food Vibrations", "Food Drop: +30")
        val prefs = Preferences(MapSettings()).apply {
            setInt("_boomBoxSongsLeft", 5)
        }
        registerItem(9919, "SongBoom BoomBox", ItemPrimaryUse.USABLE)
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.FOODDROP, Evaluator("food drop")),
                preferences = prefs,
                inventoryCount = { id -> if (id == 9919) 1 else 0 },
            ),
        )
        assertTrue(
            boosts.any {
                !it.isEquipment &&
                    it.text.contains("boombox food vibrations", ignoreCase = true)
            },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_emitsMcdEndpointBoost() {
        val prefs = Preferences(MapSettings())
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.EXPERIENCE, Evaluator("exp")),
                preferences = prefs,
                charState = CharacterState(
                    zodiacSign = "Blender",
                    equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
                    level = 15,
                    mindControlLevel = 0,
                ),
            ),
        )
        assertTrue(
            boosts.any { !it.isEquipment && it.text.startsWith("mcd ") },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_cargoEffectSourceGreysOutWhenPocketEmptied() {
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 82002,
                name = "Super Vision",
                image = "eyes.gif",
                descId = "d82002",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cargo effect Super Vision",
            ),
        )
        ModifierDatabase.injectForTest("Effect", "Super Vision", "Item Drop: +10")
        val prefs = Preferences(MapSettings()).apply {
            setBoolean(Preferences.CARGO_POCKET_EMPTIED, true)
        }
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.ITEMDROP, Evaluator("item drop")),
                preferences = prefs,
                inventoryCount = { id ->
                    if (id == net.sourceforge.kolmafia.session.BreakfastItemIds.CARGO_CULTIST_SHORTS_ID) 1 else 0
                },
            ),
        )
        assertTrue(
            boosts.any {
                !it.isEquipment &&
                    it.text.contains("cargo effect Super Vision", ignoreCase = true) &&
                    it.cmd.isEmpty()
            },
            boosts.joinToString { "${it.cmd} :: ${it.text}" },
        )
    }

    @Test
    fun build_verboseMaximizerAddsUsesRemaining() {
        ModifierDatabase.injectForTest("BoomBox", "Total Eclipse of Your Meat", "Meat Drop: +30")
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("verboseMaximizer", true)
            setInt("_boomBoxSongsLeft", 3)
        }
        registerItem(9919, "SongBoom BoomBox", ItemPrimaryUse.USABLE)
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.MEATDROP, Evaluator("meat")),
                preferences = prefs,
                inventoryCount = { id -> if (id == 9919) 1 else 0 },
            ),
        )
        assertTrue(
            boosts.any { it.text.contains("3 uses remaining") },
            boosts.joinToString { it.text },
        )
    }

    private fun nonEquipmentContext(
        spec: MaximizeSpec,
        preferences: Preferences?,
        inventoryCount: (Int) -> Int = { 0 },
        charState: CharacterState = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
            level = 15,
        ),
    ): MaximizerNonEquipmentBoosts.Context {
        val plan = MaximizerEmitSlot.Plan(
            goal = "init",
            spec = spec,
            scoreBefore = 0.0,
            scoreAfter = 100.0,
            bestPerSlot = mapOf(EquipmentSlot.HAT to ("plain hat" to 100.0)),
        )
        registerItem(8200, "plain hat", ItemPrimaryUse.HAT)
        ModifierDatabase.injectForTest("Item", "plain hat", "none")
        return MaximizerNonEquipmentBoosts.Context(
            plan = plan,
            charState = charState,
            activeEffects = emptyList(),
            inventory = MaximizerEmitSlot.InventorySnapshot(),
            inventoryCount = inventoryCount,
            gameDatabase = stubDb,
            preferences = preferences,
            mallPriceManager = null,
            priceLevel = MaximizerPriceLevel.DONT_CHECK,
        )
    }

    private fun registerFood(id: Int, name: String, fullness: Int) {
        registerItem(id, name, ItemPrimaryUse.USABLE)
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
