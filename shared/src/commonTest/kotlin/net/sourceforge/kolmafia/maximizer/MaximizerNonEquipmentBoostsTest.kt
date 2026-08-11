package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
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
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

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
        SkillDefinitionDatabase.resetForTest()
        UneffectSkillEffectMap.rebuild()
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
    fun build_foodFilterOff_suppressesEatBoost() {
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
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.MUS, Evaluator("mus")),
                preferences = Preferences(MapSettings()),
                inventoryCount = { id -> if (id == 8201) 1 else 0 },
                filters = setOf(MaximizerFilterType.CAST),
            ),
        )
        assertFalse(
            boosts.any { it.text.contains("eat 1 test food", ignoreCase = true) },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_otherFilterOff_suppressesHorseryBoost() {
        ModifierDatabase.injectForTest("Horsery", "normal horse", "Initiative: +10")
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("horseryAvailable", true)
            setString("_horsery", "")
        }
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.INITIATIVE, Evaluator("init")),
                preferences = prefs,
                filters = setOf(MaximizerFilterType.CAST, MaximizerFilterType.FOOD),
            ),
        )
        assertFalse(
            boosts.any { it.text.contains("horsery", ignoreCase = true) },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_includeAll_emitsUnavailableHorseryHint() {
        ModifierDatabase.injectForTest("Horsery", "normal horse", "Initiative: +10")
        val prefs = Preferences(MapSettings())
        val withoutIncludeAll = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.INITIATIVE, Evaluator("init")),
                preferences = prefs,
            ),
        )
        assertFalse(
            withoutIncludeAll.any { it.text.contains("get a horsery", ignoreCase = true) },
            withoutIncludeAll.joinToString { it.text },
        )
        val withIncludeAll = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.INITIATIVE, Evaluator("init")),
                preferences = prefs,
                includeAll = true,
            ),
        )
        assertTrue(
            withIncludeAll.any { it.text.contains("get a horsery", ignoreCase = true) },
            withIncludeAll.joinToString { it.text },
        )
    }

    @Test
    fun build_wishFilterOff_suppressesGenieSource() {
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 82003,
                name = "Filtered Wish Buff",
                image = "wish.gif",
                descId = "d82003",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "genie effect Filtered Wish Buff",
            ),
        )
        ModifierDatabase.injectForTest("Effect", "Filtered Wish Buff", "Muscle: +50")
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.MUS, Evaluator("mus")),
                preferences = Preferences(MapSettings()),
                filters = setOf(MaximizerFilterType.CAST),
            ),
        )
        assertFalse(
            boosts.any { it.text.contains("genie", ignoreCase = true) },
            boosts.joinToString { it.text },
        )
    }

    @Test
    fun build_castFilterOff_suppressesCastBoost() {
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 82004,
                name = "Filtered Cast Buff",
                image = "cast.gif",
                descId = "d82004",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 Filtered Cast Buff",
            ),
        )
        ModifierDatabase.injectForTest("Effect", "Filtered Cast Buff", "Muscle: +50")
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 82004,
                name = "Filtered Cast Buff",
                image = "cast.gif",
                tags = setOf("nc", "effect"),
                mpCost = 10,
                duration = 5,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        UneffectSkillEffectMap.rebuild()
        val boosts = MaximizerNonEquipmentBoosts.build(
            nonEquipmentContext(
                spec = MaximizeSpec(DoubleModifier.MUS, Evaluator("mus")),
                preferences = Preferences(MapSettings()),
                filters = setOf(MaximizerFilterType.FOOD),
            ),
        )
        assertFalse(
            boosts.any { it.text.contains("cast 1 Filtered Cast Buff", ignoreCase = true) },
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

    @Test
    fun build_liveBaselineDiffersFromPlanOverlayAtScoreCap() {
        registerFood(8201, "unused food", fullness = 1)
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 95001,
                name = "Live Rescore Mys Buff",
                image = "buff.gif",
                descId = "d95001",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 Live Rescore Mys Buff",
            ),
        )
        ModifierDatabase.injectForTest("Effect", "Live Rescore Mys Buff", "Mysticality: +100")
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 95001,
                name = "Live Rescore Mys Buff",
                image = "buff.gif",
                tags = setOf("nc", "effect"),
                mpCost = 10,
                duration = 5,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        UneffectSkillEffectMap.rebuild()
        registerItem(8202, "myst hat", ItemPrimaryUse.HAT)
        val skills = skillManagerFor("Live Rescore Mys Buff", 95001)
        val overlayPlan = MaximizerEmitSlot.Plan(
            goal = "mysticality 5 max",
            spec = MaximizeSpec(DoubleModifier.MYS, Evaluator("mysticality 5 max")),
            scoreBefore = 0.0,
            scoreAfter = 5.0,
            bestPerSlot = mapOf(EquipmentSlot.HAT to ("myst hat" to 5.0)),
        )
        val baseCtx = nonEquipmentContext(
            spec = overlayPlan.spec,
            preferences = Preferences(MapSettings()),
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
                level = 15,
            ),
            filters = setOf(MaximizerFilterType.CAST),
            skillManager = skills,
        )
        ModifierDatabase.injectForTest("Item", "plain hat", "Mysticality: +1")
        ModifierDatabase.injectForTest("Item", "myst hat", "Mysticality: +5")
        val overlayBoosts = MaximizerNonEquipmentBoosts.build(
            baseCtx.copy(
                plan = overlayPlan,
                baseline = MaximizerNonEquipmentBoosts.NonEquipmentBaseline.PLAN_OVERLAY,
            ),
        )
        val liveBoosts = MaximizerNonEquipmentBoosts.build(
            baseCtx.copy(
                plan = overlayPlan,
                baseline = MaximizerNonEquipmentBoosts.NonEquipmentBaseline.LIVE_EQUIPPED,
            ),
        )
        assertFalse(
            overlayBoosts.any { it.text.contains("Live Rescore Mys Buff", ignoreCase = true) },
            overlayBoosts.joinToString { it.text },
        )
        assertTrue(
            liveBoosts.any { it.text.contains("Live Rescore Mys Buff", ignoreCase = true) },
            liveBoosts.joinToString { it.text },
        )
    }

    private fun skillManagerFor(skillName: String, skillId: Int): SkillManager {
        val client = HttpClient(MockEngine { _ -> respond("{}", HttpStatusCode.OK) })
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skills.learnLocalSkill(
            SkillData(
                id = skillId,
                name = skillName,
                type = SkillType.NONCOMBAT,
                mpCost = 10,
                dailyLimit = 0,
                timesCast = 0,
            ),
        )
        return skills
    }

    private fun nonEquipmentContext(
        spec: MaximizeSpec,
        preferences: Preferences?,
        inventoryCount: (Int) -> Int = { 0 },
        charState: CharacterState = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
            level = 15,
        ),
        filters: Set<MaximizerFilterType> = MaximizerFilters.allEnabled(),
        includeAll: Boolean = false,
        skillManager: SkillManager? = null,
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
            filters = filters,
            includeAll = includeAll,
            skillManager = skillManager,
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
