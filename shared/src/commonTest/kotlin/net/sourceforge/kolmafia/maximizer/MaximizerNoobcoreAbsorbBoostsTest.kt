package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EquipmentData
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerNoobcoreAbsorbBoostsTest {

    private val stubDb = object : GameDatabase() {
        override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
        override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun build_emitsSkillAbsorbWithRemainingBracket() {
        registerSkillAbsorbItem()
        val boosts = MaximizerNoobcoreAbsorbBoosts.build(
            noobContext(inventoryCount = { id -> if (id == 88001) 1 else 0 }),
        )
        assertTrue(
            boosts.any {
                !it.isEquipment &&
                    it.cmd.contains("absorb") &&
                    it.text.contains("absorbs remaining")
            },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_emitsEquipmentAbsorbWithInventoryBracket() {
        registerEquipmentAbsorbItem()
        val boosts = MaximizerNoobcoreAbsorbBoosts.build(
            noobContext(inventoryCount = { id -> if (id == 88100) 1 else 0 }),
        )
        assertTrue(
            boosts.any {
                !it.isEquipment &&
                    it.text.contains("lasts til end of day") &&
                    it.text.contains("1 in inventory") &&
                    it.text.contains("absorbs remaining")
            },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun getAbsorbable_prefixesPullWhenItemInStorage() {
        registerSkillAbsorbItem()
        val ctx = noobContext(
            inventoryCount = { 0 },
        ).copy(
            inventory = MaximizerEmitSlot.InventorySnapshot(
                storageContents = mapOf(88001 to 1),
            ),
        )
        val absorbable = MaximizerNoobcoreAbsorbBoosts.getAbsorbable(88001, ctx)
        assertTrue(absorbable != null && absorbable.canMake)
        assertTrue(absorbable.cmd.contains("pull"))
        assertTrue(absorbable.cmd.contains("absorb"))
    }

    @Test
    fun nonEquipmentBuild_ordersAbsorbBeforeHorsery() {
        registerSkillAbsorbItem()
        ModifierDatabase.injectForTest("Horsery", "normal horse", "Initiative: +10")
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("horseryAvailable", true)
            setString("_horsery", "")
        }
        val boosts = MaximizerNonEquipmentBoosts.build(
            noobContext(
                preferences = prefs,
                inventoryCount = { id -> if (id == 88001) 1 else 0 },
            ),
        )
        val absorbIndex = boosts.indexOfFirst { it.text.contains("absorb noob absorb potion", ignoreCase = true) }
        val horseryIndex = boosts.indexOfFirst { it.text.contains("horsery normal horse", ignoreCase = true) }
        assertTrue(absorbIndex >= 0 && horseryIndex >= 0)
        assertTrue(absorbIndex < horseryIndex)
    }

    private fun registerSkillAbsorbItem() {
        registerBaselineHat()
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 23001,
                name = "Bendable Knees",
                image = "skill.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
        ModifierDatabase.injectForTest("Skill", "Bendable Knees", "Initiative: +50")
        ItemDatabase.registerForTest(
            ItemData(
                id = 88001,
                name = "noob absorb potion",
                descId = "0",
                image = "potion.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun registerEquipmentAbsorbItem() {
        registerBaselineHat()
        val name = "absorb hat"
        ItemDatabase.registerForTest(
            ItemData(
                id = 88100,
                name = name,
                descId = "d88100",
                image = "hat.gif",
                primaryUse = ItemPrimaryUse.HAT,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(88100, EquipmentData(name = name, power = 10))
        ModifierDatabase.injectForTest("Item", name, "Initiative: +25")
    }

    private fun registerBaselineHat() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 8200,
                name = "plain hat",
                descId = "d8200",
                image = "hat.gif",
                primaryUse = ItemPrimaryUse.HAT,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", "plain hat", "none")
    }

    private fun noobContext(
        preferences: Preferences? = Preferences(MapSettings()),
        inventoryCount: (Int) -> Int = { 0 },
    ): MaximizerNonEquipmentBoosts.Context {
        val spec = MaximizeSpec(DoubleModifier.INITIATIVE, Evaluator("init"))
        val plan = MaximizerEmitSlot.Plan(
            goal = "init",
            spec = spec,
            scoreBefore = 0.0,
            scoreAfter = 100.0,
            bestPerSlot = mapOf(EquipmentSlot.HAT to ("plain hat" to 100.0)),
        )
        return MaximizerNonEquipmentBoosts.Context(
            plan = plan,
            charState = CharacterState(
                challengePath = "Gelatinous Noob",
                equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
                level = 10,
                absorbs = 0,
            ),
            activeEffects = emptyList(),
            inventory = MaximizerEmitSlot.InventorySnapshot(),
            inventoryCount = inventoryCount,
            gameDatabase = stubDb,
            preferences = preferences,
            mallPriceManager = null,
            priceLevel = MaximizerPriceLevel.DONT_CHECK,
        )
    }
}
