package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData as StaticEffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class MaximizerBoostSourceRulesTest {

    private val stubDb = object : GameDatabase() {
        override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
        override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EffectDatabase.resetForTest()
        ModifierDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        PocketDatabase.resetForTest()
        UneffectSkillEffectMap.resetForTest()
    }

    @Test
    fun castRule_greysOutWhenSkillNotOwned() {
        registerPatienceEffect()
        val ctx = ruleContext(
            source = "cast 1 Patience of the Tortoise",
            effectId = 22,
            effectName = "Patience of the Tortoise",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun castRule_emitsCostsWhenSkillOwned() {
        registerPatienceEffect()
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 2000,
                name = "Patience of the Tortoise",
                image = "tort.gif",
                tags = setOf("nc", "effect", "other"),
                mpCost = 15,
                duration = 5,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        UneffectSkillEffectMap.rebuild()
        val client = HttpClient(MockEngine { _ -> respond("{}", HttpStatusCode.OK) })
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skills.learnLocalSkill(
            SkillData(
                id = 2000,
                name = "Patience of the Tortoise",
                type = SkillType.NONCOMBAT,
                mpCost = 15,
                dailyLimit = 0,
                timesCast = 0,
            ),
        )
        val ctx = ruleContext(
            source = "cast 1 Patience of the Tortoise",
            effectId = 22,
            effectName = "Patience of the Tortoise",
            skillManager = skills,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("cast 1 Patience of the Tortoise", result.cmd)
        assertEquals(5, result.duration)
        assertEquals(Int.MAX_VALUE, result.usesRemaining)
    }

    @Test
    fun cargoRule_greysOutWhenDailyPocketEmptied() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean(Preferences.CARGO_POCKET_EMPTIED, true)
        }
        val ctx = ruleContext(
            source = "cargo effect Super Vision",
            effectId = 100,
            effectName = "Super Vision",
            preferences = prefs,
            inventoryCount = { id ->
                when (id) {
                    BreakfastItemIds.CARGO_CULTIST_SHORTS_ID -> 1
                    else -> 0
                }
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun cargoRule_usesUnpickedPocketDuration() {
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 100,
                name = "Super Vision",
                image = "eyes.gif",
                descId = "d100",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cargo effect Super Vision",
            ),
        )
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest(
                """
                5	Effect	Super Vision (40)
                15	Effect	Super Vision (20)
                """.trimIndent(),
            ),
        )
        val ctx = ruleContext(
            source = "cargo effect Super Vision",
            effectId = 100,
            effectName = "Super Vision",
            inventoryCount = { id ->
                if (id == BreakfastItemIds.CARGO_CULTIST_SHORTS_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("cargo effect Super Vision", result.cmd)
        assertEquals(40, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun alliedRadioRule_reportsUsesRemaining() {
        val prefs = Preferences(MapSettings()).apply {
            setInt(Preferences.ALLIED_RADIO_DROPS_USED, 1)
        }
        registerItem(BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID, "Allied Radio Backpack", ItemPrimaryUse.USABLE)
        val ctx = ruleContext(
            source = "alliedradio effect boon",
            effectId = 2999,
            effectName = "Wildsun Boon",
            preferences = prefs,
            inventoryCount = { id ->
                when (id) {
                    BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID -> 1
                    BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID -> 0
                    else -> 0
                }
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(100, result.duration)
        assertEquals(2, result.usesRemaining)
    }

    @Test
    fun barrelPrayerRule_greysOutWhenAlreadyUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("barrelShrineUnlocked", true)
            setBoolean("_barrelPrayer", true)
        }
        val ctx = ruleContext(
            source = "barrelprayer buff",
            effectId = 1945,
            effectName = "Barrel Chested",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(50, result.duration)
    }

    @Test
    fun friarsRule_greysOutWhenBlessingReceived() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("lastFriarCeremonyAscension", 10)
            setInt("knownAscensions", 10)
            setBoolean("friarsBlessingReceived", true)
        }
        val ctx = ruleContext(
            source = "friars food",
            effectId = 459,
            effectName = "Brother Flying Burrito's Blessing",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(20, result.duration)
    }

    @Test
    fun friarsRule_skipsWhenAscensionMismatch() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("lastFriarCeremonyAscension", 5)
            setInt("knownAscensions", 10)
        }
        val ctx = ruleContext(
            source = "friars food",
            effectId = 459,
            effectName = "Brother Flying Burrito's Blessing",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun pillkeeperRule_includeAllHintWhenMissingItem() {
        val ctx = ruleContext(
            source = "pillkeeper",
            effectId = 2000,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("(get an Eight Days a Week Pill Keeper)", result.text)
    }

    @Test
    fun pillkeeperRule_greysOutWhenFreeUsedAndLowSpleen() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_freePillKeeperUsed", true)
        }
        registerItem(10333, "Eight Days a Week Pill Keeper", ItemPrimaryUse.ACCESSORY)
        val ctx = ruleContext(
            source = "pillkeeper",
            effectId = 2000,
            effectName = "Test Effect",
            preferences = prefs,
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
                level = 15,
                spleenUsed = 13,
                spleenLimit = 15,
            ),
            inventoryCount = { id -> if (id == 10333) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(2, result.usesRemaining)
        assertEquals(3, result.extraCosts?.spleen)
    }

    @Test
    fun poolRule_usesRemainingFromPoolGamesPref() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_poolGames", 2)
        }
        registerItem(BreakfastManager.VIP_LOUNGE_KEY_ID, "VIP lounge key", ItemPrimaryUse.USABLE)
        val ctx = ruleContext(
            source = "pool 1",
            effectId = 626,
            effectName = "Billiards Belligerence",
            preferences = prefs,
            inventoryCount = { id ->
                if (id == BreakfastManager.VIP_LOUNGE_KEY_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("pool 1", result.cmd)
        assertEquals(1, result.usesRemaining)
        assertEquals(10, result.duration)
    }

    @Test
    fun poolRule_includeAllLoungeHintWithoutVipKey() {
        val ctx = ruleContext(
            source = "pool 1",
            effectId = 626,
            effectName = "Billiards Belligerence",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("( get access to the VIP lounge )", result.text)
    }

    @Test
    fun showerRule_greysOutWhenAlreadyUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_aprilShower", true)
        }
        registerItem(BreakfastManager.VIP_LOUNGE_KEY_ID, "VIP lounge key", ItemPrimaryUse.USABLE)
        val ctx = ruleContext(
            source = "shower warm",
            effectId = 830,
            effectName = "Muscle Unbound",
            preferences = prefs,
            inventoryCount = { id ->
                if (id == BreakfastManager.VIP_LOUNGE_KEY_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(50, result.duration)
    }

    @Test
    fun swimRule_greysOutWhenAlreadyUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_olympicSwimmingPool", true)
        }
        registerItem(BreakfastManager.VIP_LOUNGE_KEY_ID, "VIP lounge key", ItemPrimaryUse.USABLE)
        val ctx = ruleContext(
            source = "swim laps",
            effectId = 1032,
            effectName = "Lapdog",
            preferences = prefs,
            inventoryCount = { id ->
                if (id == BreakfastManager.VIP_LOUNGE_KEY_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun jukeboxRule_skipsWhenHardcore() {
        val ctx = ruleContext(
            source = "jukebox item",
            effectId = 233,
            effectName = "Techno Bliss",
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
                level = 15,
                isHardcore = true,
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun ballpitRule_greysOutWhenAlreadyUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_ballpit", true)
        }
        val ctx = ruleContext(
            source = "ballpit",
            effectId = 712,
            effectName = "Having a Ball!",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(20, result.duration)
    }

    private fun registerPatienceEffect() {
        EffectDatabase.registerForTest(
            StaticEffectData(
                id = 22,
                name = "Patience of the Tortoise",
                image = "tort.gif",
                descId = "d22",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 Patience of the Tortoise",
            ),
        )
        UneffectSkillEffectMap.rebuild()
    }

    private fun ruleContext(
        source: String,
        effectId: Int,
        effectName: String,
        preferences: Preferences = Preferences(MapSettings()),
        inventoryCount: (Int) -> Int = { 0 },
        skillManager: SkillManager? = null,
        charState: CharacterState? = null,
        includeAll: Boolean = false,
    ): MaximizerBoostSourceRules.SourceRuleContext {
        val plan = MaximizerEmitSlot.Plan(
            goal = "mus",
            spec = MaximizeSpec(DoubleModifier.MUS, Evaluator("mus")),
            scoreBefore = 0.0,
            scoreAfter = 100.0,
            bestPerSlot = mapOf(EquipmentSlot.HAT to ("plain hat" to 100.0)),
        )
        registerItem(8200, "plain hat", ItemPrimaryUse.HAT)
        ModifierDatabase.injectForTest("Item", "plain hat", "none")
        return MaximizerBoostSourceRules.SourceRuleContext(
            base = MaximizerNonEquipmentBoosts.Context(
                plan = plan,
                charState = charState ?: CharacterState(
                    equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
                    level = 15,
                ),
                activeEffects = emptyList(),
                inventory = MaximizerEmitSlot.InventorySnapshot(),
                inventoryCount = inventoryCount,
                gameDatabase = stubDb,
                preferences = preferences,
                mallPriceManager = null,
                priceLevel = MaximizerPriceLevel.DONT_CHECK,
                skillManager = skillManager,
                includeAll = includeAll,
            ),
            effectId = effectId,
            effectName = effectName,
            source = source,
            hasEffect = false,
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
