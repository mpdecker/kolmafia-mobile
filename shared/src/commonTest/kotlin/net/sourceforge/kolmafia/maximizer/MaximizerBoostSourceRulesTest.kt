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
import net.sourceforge.kolmafia.character.AscensionPath
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
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.data.CafeDatabase
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.session.DemonTypes
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.session.BeachHeadAvailability
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.session.RabbitHoleAvailability
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState

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
        CandyDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        CafeDatabase.resetForTest()
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

    @Test
    fun telescopeRule_includeAllHintWhenNoUpgrades() {
        val ctx = ruleContext(
            source = "telescope high",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("( get a telescope )", result.text)
        assertEquals(10, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun telescopeRule_greysOutWhenLookedHigh() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("telescopeUpgrades", 1)
            setBoolean("telescopeLookedHigh", true)
        }
        val ctx = ruleContext(
            source = "telescope high",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun fortuneRule_includeAllLoungeHintWithoutVipKey() {
        val ctx = ruleContext(
            source = "fortune buff",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("( get access to the VIP lounge )", result.text)
        assertEquals(100, result.duration)
    }

    @Test
    fun fortuneRule_greysOutWhenAlreadyUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_clanFortuneBuffUsed", true)
        }
        registerItem(BreakfastManager.VIP_LOUNGE_KEY_ID, "VIP lounge key", ItemPrimaryUse.USABLE)
        val ctx = ruleContext(
            source = "fortune buff",
            effectId = 100,
            effectName = "Test Effect",
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
    fun momRule_skipsWhenQuestNotFinished() {
        val ctx = ruleContext(
            source = "mom food",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun momRule_greysOutWhenFoodReceived() {
        val prefs = Preferences(MapSettings()).apply {
            setString(Quest.SEA_MONKEES.prefKey, QuestDatabase.FINISHED)
            setBoolean("_momFoodReceived", true)
        }
        val ctx = ruleContext(
            source = "mom food",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(50, result.duration)
    }

    @Test
    fun concertRule_availableForFratMatchingSong() {
        val prefs = Preferences(MapSettings()).apply {
            setString("sidequestArenaCompleted", "fratboy")
        }
        val ctx = ruleContext(
            source = "concert Elvish Flea",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("concert Elvish Flea", result.cmd)
        assertEquals(20, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun concertRule_skipsWhenSongDoesNotMatchSide() {
        val prefs = Preferences(MapSettings()).apply {
            setString("sidequestArenaCompleted", "hippy")
        }
        val ctx = ruleContext(
            source = "concert Elvish Flea",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun summonRule_greysOutWhenDemonNameEmpty() {
        val prefs = Preferences(MapSettings()).apply {
            setString(Quest.MANOR.prefKey, QuestDatabase.FINISHED)
        }
        val ctx = ruleContext(
            source = "summon 3",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id ->
                when (id) {
                    DemonTypes.EVIL_SCROLL -> 1
                    DemonTypes.BLACK_CANDLE -> 3
                    else -> 0
                }
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(30, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun summonRule_greysOutWhenAlreadySummoned() {
        val prefs = Preferences(MapSettings()).apply {
            setString(Quest.MANOR.prefKey, QuestDatabase.FINISHED)
            setBoolean(Preferences.DEMON_SUMMONED, true)
        }
        val ctx = ruleContext(
            source = "summon 1",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id ->
                when (id) {
                    DemonTypes.EVIL_SCROLL -> 1
                    DemonTypes.BLACK_CANDLE -> 3
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
    fun mayosoakRule_includeAllHintWhenWorkshedNotMayoClinic() {
        val ctx = ruleContext(
            source = "mayosoak",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("( install portable Mayo Clinic )", result.text)
        assertEquals(20, result.duration)
    }

    @Test
    fun mayosoakRule_greysOutWhenAlreadySoaked() {
        val prefs = Preferences(MapSettings()).apply {
            setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
            setBoolean("_mayoTankSoaked", true)
        }
        val ctx = ruleContext(
            source = "mayosoak",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun witchessRule_includeAllHintWhenNotInstalled() {
        val ctx = ruleContext(
            source = "witchess",
            effectId = 100,
            effectName = "Chess Puzzle Buff",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("(install Witchess Set for Chess Puzzle Buff)", result.text)
        assertEquals(25, result.duration)
    }

    @Test
    fun witchessRule_greysOutWhenBuffAlreadyUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean(CampgroundItemSync.CAMPGROUND_HAS_WITCHESS_SET_PREF, true)
            setInt("puzzleChampBonus", 20)
            setBoolean("_witchessBuff", true)
        }
        val ctx = ruleContext(
            source = "witchess",
            effectId = 100,
            effectName = "Chess Puzzle Buff",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun witchessRule_manualHintWhenPuzzleChampBonusNotMaxed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean(CampgroundItemSync.CAMPGROUND_HAS_WITCHESS_SET_PREF, true)
            setInt("puzzleChampBonus", 5)
        }
        val ctx = ruleContext(
            source = "witchess",
            effectId = 100,
            effectName = "Chess Puzzle Buff",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("(manually get Chess Puzzle Buff)", result.text)
    }

    @Test
    fun monorailRule_greysOutWhenAlreadyFavored() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_lyleFavored", true)
        }
        val ctx = ruleContext(
            source = "monorail buff",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(10, result.duration)
    }

    @Test
    fun toggleRule_skipsWithoutInterestEffects() {
        val ctx = ruleContext(
            source = "toggle Become Intensely interested",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun toggleRule_preservesCmdWithSuperficiallyInterested() {
        val ctx = ruleContext(
            source = "toggle Become Intensely interested",
            effectId = 100,
            effectName = "Test Effect",
            activeEffects = listOf(EffectData(id = 2288, name = "Become Superficially interested", duration = 10)),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("toggle Become Intensely interested", result.cmd)
    }

    @Test
    fun crossstreamsRule_includeAllHintWithoutProtonPack() {
        val ctx = ruleContext(
            source = "crossstreams",
            effectId = 100,
            effectName = "Cross the Streams",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("(acquire protonic accelerator pack and crossstreams for Cross the Streams)", result.text)
        assertEquals(10, result.duration)
    }

    @Test
    fun crossstreamsRule_greysOutWhenAlreadyCrossed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_streamsCrossed", true)
        }
        val ctx = ruleContext(
            source = "crossstreams",
            effectId = 100,
            effectName = "Cross the Streams",
            preferences = prefs,
            inventoryCount = { id -> if (id == 9082) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun monkeyPawRule_includeAllHintWhenMissingPaw() {
        val ctx = ruleContext(
            source = "monkeypaw effect buff",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("( acquire a cursed monkey's paw )", result.text)
    }

    @Test
    fun monkeyPawRule_greysOutWhenWishesExhausted() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_monkeyPawWishesUsed", 5)
        }
        val ctx = ruleContext(
            source = "monkeypaw effect buff",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id -> if (id == 11186) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(30, result.duration)
    }

    @Test
    fun genieRule_usesRemainingFromBottleWishes() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_genieWishesUsed", 1)
        }
        val ctx = ruleContext(
            source = "genie effect buff",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id ->
                when (id) {
                    BreakfastItemIds.GENIE_BOTTLE_ID -> 1
                    else -> 0
                }
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("genie effect buff", result.cmd)
        assertEquals(2, result.usesRemaining)
        assertEquals(20, result.duration)
    }

    @Test
    fun genieRule_usesRemainingFromPocketWishOnly() {
        val ctx = ruleContext(
            source = "genie effect buff",
            effectId = 100,
            effectName = "Test Effect",
            inventoryCount = { id -> if (id == 9537) 2 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("genie effect buff", result.cmd)
        assertEquals(2, result.usesRemaining)
    }

    @Test
    fun genieRule_skipsWhenNoWishesWithoutIncludeAll() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_genieWishesUsed", 3)
        }
        val ctx = ruleContext(
            source = "genie effect buff",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id ->
                if (id == BreakfastItemIds.GENIE_BOTTLE_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun gongRule_setsDurationAndAdvCost() {
        val ctx = ruleContext(
            source = "gong Test Effect",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("gong Test Effect", result.cmd)
        assertEquals(20, result.duration)
        assertEquals(3, result.extraCosts?.adv)
    }

    @Test
    fun styxRule_skipsNonBadMoon() {
        val ctx = ruleContext(
            source = "styx spray",
            effectId = 100,
            effectName = "Test Effect",
            charState = CharacterState(zodiacSign = "Mongoose"),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun styxRule_availableOnBadMoon() {
        val ctx = ruleContext(
            source = "styx spray",
            effectId = 100,
            effectName = "Test Effect",
            charState = CharacterState(zodiacSign = "Bad Moon"),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("styx spray", result.cmd)
        assertEquals(10, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun styxRule_greysOutWhenPixieVisited() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("styxPixieVisited", true)
        }
        val ctx = ruleContext(
            source = "styx spray",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            charState = CharacterState(zodiacSign = "Bad Moon"),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(10, result.duration)
    }

    @Test
    fun playRule_skipsWithoutDeck() {
        val ctx = ruleContext(
            source = "play",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun playRule_includeAllAcquireHint() {
        val ctx = ruleContext(
            source = "play",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text!!.contains("acquire Deck of Every Card"))
    }

    @Test
    fun playRule_usesRemainingFromCardsDrawn() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_deckCardsDrawn", 5)
        }
        val ctx = ruleContext(
            source = "play",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id -> if (id == 8382) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("play", result.cmd)
        assertEquals(2, result.usesRemaining)
        assertEquals(20, result.duration)
    }

    @Test
    fun playRule_greysOutWhenCardsDrawnExhausted() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_deckCardsDrawn", 11)
        }
        val ctx = ruleContext(
            source = "play",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            inventoryCount = { id -> if (id == 8382) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
    }

    @Test
    fun playRule_acceptsLoEReplicaDeck() {
        val ctx = ruleContext(
            source = "play",
            effectId = 100,
            effectName = "Test Effect",
            charState = CharacterState(challengePath = "Legacy of Loathing"),
            inventoryCount = { id -> if (id == 11230) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals("play", result.cmd)
    }

    @Test
    fun skeletonRule_setsDuration() {
        val ctx = ruleContext(
            source = "skeleton Test Effect",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("skeleton Test Effect", result.cmd)
        assertEquals(30, result.duration)
    }

    @Test
    fun gapRule_skipsWithoutPants() {
        registerItem(4696, "Greatest American Pants", ItemPrimaryUse.PANTS)
        val ctx = ruleContext(
            source = "gap Super Skill",
            effectId = 100,
            effectName = "Super Skill",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun gapRule_includeAllAcquireHint() {
        registerItem(4696, "Greatest American Pants", ItemPrimaryUse.PANTS)
        val ctx = ruleContext(
            source = "gap Super Skill",
            effectId = 100,
            effectName = "Super Skill",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text!!.contains("Greatest American Pants"))
    }

    @Test
    fun gapRule_greysOutAtFiveBuffs() {
        registerItem(4696, "Greatest American Pants", ItemPrimaryUse.PANTS)
        val prefs = Preferences(MapSettings()).apply { setInt("_gapBuffs", 5) }
        val ctx = ruleContext(
            source = "gap Super Skill",
            effectId = 100,
            effectName = "Super Skill",
            preferences = prefs,
            inventoryCount = { id -> if (id == 4696) 1 else 0 },
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.PANTS to "Greatest American Pants"),
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun gapRule_equipHintWhenNotWorn() {
        registerItem(4696, "Greatest American Pants", ItemPrimaryUse.PANTS)
        val ctx = ruleContext(
            source = "gap Super Skill",
            effectId = 100,
            effectName = "Super Skill",
            inventoryCount = { id -> if (id == 4696) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text!!.contains("equip Greatest American Pants"))
    }

    @Test
    fun gapRule_superSkillDuration() {
        registerItem(4696, "Greatest American Pants", ItemPrimaryUse.PANTS)
        val ctx = ruleContext(
            source = "gap Super Skill",
            effectId = 100,
            effectName = "Super Skill",
            inventoryCount = { id -> if (id == 4696) 1 else 0 },
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.PANTS to "Greatest American Pants"),
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(5, result.duration)
    }

    @Test
    fun spacegateRule_skipsKingdomOfExploathing() {
        val ctx = ruleContext(
            source = "spacegate vaccine 1",
            effectId = 100,
            effectName = "Test Effect",
            charState = CharacterState(challengePath = "Kingdom of Exploathing"),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun spacegateRule_includeAllWhenVaccineUnavailable() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("spacegateAlways", true)
        }
        val ctx = ruleContext(
            source = "spacegate vaccine 1",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text!!.contains("unlock Spacegate"))
    }

    @Test
    fun daycareRule_greysOutWhenSpaUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("daycareOpen", true)
            setBoolean("_daycareSpa", true)
        }
        val ctx = ruleContext(
            source = "daycare",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(100, result.duration)
    }

    @Test
    fun vault3Rule_skipsNonNuclearAutumn() {
        val prefs = Preferences(MapSettings()).apply { setInt("falloutShelterLevel", 5) }
        val ctx = ruleContext(
            source = "campground vault3",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun vault3Rule_availableOnNuclearAutumn() {
        val prefs = Preferences(MapSettings()).apply { setInt("falloutShelterLevel", 5) }
        val ctx = ruleContext(
            source = "campground vault3",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            charState = CharacterState(challengePath = "Nuclear Autumn"),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("campground vault3", result.cmd)
        assertEquals(1, result.usesRemaining)
        assertEquals(100, result.duration)
    }

    @Test
    fun grimRule_includeAllWhenMissingFamiliar() {
        val ctx = ruleContext(
            source = "grim",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text!!.contains("Grim Brother"))
    }

    @Test
    fun grimRule_greysOutWhenGrimBuffUsed() {
        val prefs = Preferences(MapSettings()).apply { setBoolean("_grimBuff", true) }
        val grim = FamiliarData(
            id = 179,
            name = "Grim Brother",
            race = "Grim Brother",
            weight = 1,
            experience = 0,
            kills = 0,
        )
        val ctx = ruleContext(
            source = "grim",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
            familiarManager = makeFamiliarManager(FamiliarState(ownedFamiliars = listOf(grim))),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun aprilbandRule_passesThroughCmd() {
        val ctx = ruleContext(
            source = "aprilband effect nc",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("aprilband effect nc", result.cmd)
    }

    @Test
    fun terminalEnhanceRule_skipsWithoutTerminal() {
        val ctx = ruleContext(
            source = "terminal enhance Test Effect",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun terminalEnhanceRule_includeAllInstallHint() {
        val ctx = ruleContext(
            source = "terminal enhance Test Effect",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text.orEmpty().contains("install Source Terminal"))
    }

    @Test
    fun terminalEnhanceRule_greysOutAtUseLimit() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
            setString("sourceTerminalEnhanceKnown", "Test Effect")
            setInt("_sourceTerminalEnhanceUses", 1)
        }
        val ctx = ruleContext(
            source = "terminal enhance Test Effect",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun terminalEnhanceRule_ingramAndPramDuration() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
            setString("sourceTerminalChips", "INGRAM,CRAM")
            setString("sourceTerminalEnhanceKnown", "Test Effect")
            setInt("sourceTerminalPram", 2)
        }
        val ctx = ruleContext(
            source = "terminal enhance Test Effect",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(60, result.duration)
        assertEquals(2, result.usesRemaining)
    }

    @Test
    fun campAwayCloudRule_skipsWhenTentUnavailable() {
        val ctx = ruleContext(
            source = "campaway cloud",
            effectId = 2499,
            effectName = "That's Just Cloud-Talk, Man",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun campAwayCloudRule_greysOutWhenCloudBuffUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("getawayCampsiteUnlocked", true)
            setInt("_campAwayCloudBuffs", 1)
        }
        val ctx = ruleContext(
            source = "campaway cloud",
            effectId = 2499,
            effectName = "That's Just Cloud-Talk, Man",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun campAwayCloudRule_usesRemainingWhenAvailable() {
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("getawayCampsiteUnlocked", true)
        }
        val ctx = ruleContext(
            source = "campaway cloud",
            effectId = 2499,
            effectName = "That's Just Cloud-Talk, Man",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("campaway cloud", result.cmd)
        assertEquals(1, result.usesRemaining)
        assertEquals(100, result.duration)
    }

    @Test
    fun loathingIdolRule_passesThroughCmd() {
        val ctx = ruleContext(
            source = "loathingidol pop",
            effectId = 2814,
            effectName = "Poppy Performance",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("loathingidol pop", result.cmd)
        assertEquals(30, result.duration)
    }

    @Test
    fun mayamRule_greysOutUnavailableResonance() {
        val prefs = Preferences(MapSettings()).apply {
            setString("_mayamSymbolsUsed", "eye,yam1,eyepatch,yam2")
        }
        val ctx = ruleContext(
            source = "mayam resonance mayam spinach",
            effectId = 310,
            effectName = "Pop-eyed",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
    }

    @Test
    fun mayamRule_passesThroughAvailableResonance() {
        val ctx = ruleContext(
            source = "mayam resonance mayam spinach",
            effectId = 310,
            effectName = "Pop-eyed",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("mayam resonance mayam spinach", result.cmd)
    }

    @Test
    fun asdonMartinDriveRule_skipsWithoutWorkshed() {
        val ctx = ruleContext(
            source = "asdonmartin drive Test Effect",
            effectId = 100,
            effectName = "Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun asdonMartinDriveRule_includeAllInstallHint() {
        val ctx = ruleContext(
            source = "asdonmartin drive Test Effect",
            effectId = 100,
            effectName = "Test Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text.orEmpty().contains("install Asdon Martin"))
    }

    @Test
    fun asdonMartinDriveRule_greysOutWhenFuelLow() {
        val prefs = Preferences(MapSettings()).apply {
            setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, CampgroundItemSync.ASDON_MARTIN_ID)
            setInt(CampgroundItemSync.ASDON_MARTIN_FUEL_PREF, 10)
        }
        val ctx = ruleContext(
            source = "asdonmartin drive Test Effect",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
        assertEquals(37, result.extraCosts?.fuel)
    }

    @Test
    fun asdonMartinDriveRule_usesRemainingFromFuel() {
        val prefs = Preferences(MapSettings()).apply {
            setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, CampgroundItemSync.ASDON_MARTIN_ID)
            setInt(CampgroundItemSync.ASDON_MARTIN_FUEL_PREF, 111)
        }
        val ctx = ruleContext(
            source = "asdonmartin drive Test Effect",
            effectId = 100,
            effectName = "Test Effect",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("asdonmartin drive Test Effect", result.cmd)
        assertEquals(3, result.usesRemaining)
        assertEquals(37, result.extraCosts?.fuel)
    }

    @Test
    fun mayamAvailability_listsFreshResonance() {
        val available = MayamAvailability.availableResonances(Preferences(MapSettings()))
        assertTrue(available.contains("mayam spinach"))
    }

    @Test
    fun beachHeadRule_skipsWhenPathBlocksBeachComb() {
        registerItem(BeachHeadAvailability.BEACH_COMB_ID, "Beach Comb", ItemPrimaryUse.ACCESSORY)
        ModifierDatabase.injectForTest(
            "Item",
            "Beach Comb",
            """Last Available: "2019-07"""",
        )
        val ctx = ruleContext(
            source = "beach head Hot-Headed",
            effectId = 100,
            effectName = "Hot-Headed",
            charState = CharacterState(
                challengePath = AscensionPath.THRIFTY.apiName,
                level = 15,
            ),
            inventoryCount = { id ->
                if (id == BeachHeadAvailability.BEACH_COMB_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun beachHeadRule_skipsWithoutComb() {
        val ctx = ruleContext(
            source = "beach head Hot-Headed",
            effectId = 100,
            effectName = "Hot-Headed",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun beachHeadRule_includeAllCombHint() {
        val ctx = ruleContext(
            source = "beach head Hot-Headed",
            effectId = 100,
            effectName = "Hot-Headed",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text.orEmpty().contains("Beach Comb"))
    }

    @Test
    fun beachHeadRule_greysOutWhenHeadUsed() {
        registerItem(BeachHeadAvailability.BEACH_COMB_ID, "Beach Comb", ItemPrimaryUse.ACCESSORY)
        val prefs = Preferences(MapSettings()).apply {
            setString("_beachHeadsUsed", "1")
        }
        val ctx = ruleContext(
            source = "beach head Hot-Headed",
            effectId = 100,
            effectName = "Hot-Headed",
            preferences = prefs,
            inventoryCount = { id ->
                if (id == BeachHeadAvailability.BEACH_COMB_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun beachHeadRule_passesWhenHeadAvailable() {
        registerItem(BeachHeadAvailability.BEACH_COMB_ID, "Beach Comb", ItemPrimaryUse.ACCESSORY)
        val ctx = ruleContext(
            source = "beach head Hot-Headed",
            effectId = 100,
            effectName = "Hot-Headed",
            inventoryCount = { id ->
                if (id == BeachHeadAvailability.BEACH_COMB_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("beach head Hot-Headed", result.cmd)
        assertEquals(50, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun skateRule_skipsWhenStatusMismatch() {
        val prefs = Preferences(MapSettings()).apply {
            setString("skateParkStatus", "peace")
        }
        val ctx = ruleContext(
            source = "skate lutz, the ice skate",
            effectId = 100,
            effectName = "Skate Buff",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun skateRule_greysOutWhenBuffUsed() {
        val prefs = Preferences(MapSettings()).apply {
            setString("skateParkStatus", "ice")
            setBoolean("_skateBuff1", true)
        }
        val ctx = ruleContext(
            source = "skate lutz, the ice skate",
            effectId = 100,
            effectName = "Skate Buff",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun skateRule_passesWhenStatusMatches() {
        val prefs = Preferences(MapSettings()).apply {
            setString("skateParkStatus", "ice")
        }
        val ctx = ruleContext(
            source = "skate lutz, the ice skate",
            effectId = 100,
            effectName = "Skate Buff",
            preferences = prefs,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("skate lutz, the ice skate", result.cmd)
        assertEquals(30, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun hatterRule_skipsWithoutPotionOrEffect() {
        val ctx = ruleContext(
            source = "hatter 10",
            effectId = 100,
            effectName = "Smoky Third Eye",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun hatterRule_skipsWhenHatLengthUnavailable() {
        val ctx = ruleContext(
            source = "hatter 99",
            effectId = 100,
            effectName = "Smoky Third Eye",
            inventoryCount = { id ->
                if (id == RabbitHoleAvailability.DRINK_ME_POTION_ID) 1 else 0
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun hatterRule_greysOutWhenTeaPartyUsed() {
        registerItem(8210, "SmokyThird", ItemPrimaryUse.HAT)
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("_madTeaParty", true)
        }
        val ctx = ruleContext(
            source = "hatter 10",
            effectId = 100,
            effectName = "Smoky Third Eye",
            preferences = prefs,
            inventoryCount = { id ->
                when (id) {
                    RabbitHoleAvailability.DRINK_ME_POTION_ID -> 1
                    8210 -> 1
                    else -> 0
                }
            },
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.HAT to "SmokyThird"),
                level = 15,
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals(0, result.usesRemaining)
    }

    @Test
    fun hatterRule_passesWhenHatLengthAvailable() {
        registerItem(8210, "SmokyThird", ItemPrimaryUse.HAT)
        val ctx = ruleContext(
            source = "hatter 10",
            effectId = 100,
            effectName = "Smoky Third Eye",
            inventoryCount = { id ->
                when (id) {
                    RabbitHoleAvailability.DRINK_ME_POTION_ID -> 1
                    8210 -> 1
                    else -> 0
                }
            },
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.HAT to "SmokyThird"),
                level = 15,
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("hatter 10", result.cmd)
        assertEquals(30, result.duration)
        assertEquals(1, result.usesRemaining)
    }

    @Test
    fun synthesizeRule_skipsInGLover() {
        val ctx = ruleContext(
            source = "synthesize Hot Synthesis",
            effectId = 2165,
            effectName = "Hot Synthesis",
            charState = CharacterState(
                challengePath = AscensionPath.GLOVER.apiName,
                level = 15,
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun synthesizeRule_skipsWithoutSkill() {
        val ctx = ruleContext(
            source = "synthesize Hot Synthesis",
            effectId = 2165,
            effectName = "Hot Synthesis",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun synthesizeRule_includeAllSkillHint() {
        val ctx = ruleContext(
            source = "synthesize Hot Synthesis",
            effectId = 2165,
            effectName = "Hot Synthesis",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertTrue(result.text.orEmpty().contains("Sweet Synthesis"))
    }

    @Test
    fun synthesizeRule_greysOutWithoutSpleen() {
        val skills = sweetSynthesisSkillManager()
        val ctx = ruleContext(
            source = "synthesize Hot Synthesis",
            effectId = 2165,
            effectName = "Hot Synthesis",
            skillManager = skills,
            charState = CharacterState(
                level = 15,
                spleenUsed = 4,
                spleenLimit = 4,
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
    }

    @Test
    fun synthesizeRule_greysOutWithoutCandyPair() {
        val skills = sweetSynthesisSkillManager()
        val ctx = ruleContext(
            source = "synthesize Hot Synthesis",
            effectId = 2165,
            effectName = "Hot Synthesis",
            skillManager = skills,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
    }

    @Test
    fun synthesizeRule_passesWithCandyPairAndSpleen() {
        registerCandy(9100, "test candy a")
        registerCandy(9105, "test candy b")
        val skills = sweetSynthesisSkillManager()
        val ctx = ruleContext(
            source = "synthesize Hot Synthesis",
            effectId = 2165,
            effectName = "Hot Synthesis",
            skillManager = skills,
            charState = CharacterState(level = 15, spleenUsed = 0, spleenLimit = 4),
            inventoryCount = { id ->
                when (id) {
                    9100, 9105 -> 2
                    else -> 0
                }
            },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("synthesize Hot Synthesis", result.cmd)
        assertEquals(30, result.duration)
        assertEquals(4, result.usesRemaining)
        assertEquals(1, result.extraCosts?.spleen)
    }

    @Test
    fun consumptionRule_eatIncludesFullnessCost() {
        registerItem(9001, "test maximizer food", ItemPrimaryUse.FOOD)
        registerConsumable("test maximizer food", ConsumableType.FOOD, 3)
        val ctx = ruleContext(
            source = "eat 1 test maximizer food",
            effectId = 100,
            effectName = "Test Food Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals("eat 1 test maximizer food", result.cmd)
    }

    @Test
    fun consumptionRule_drinkPassesThrough() {
        registerItem(9002, "test maximizer booze", ItemPrimaryUse.DRINK)
        registerConsumable("test maximizer booze", ConsumableType.DRINK, 2)
        val ctx = ruleContext(
            source = "drink 1 test maximizer booze",
            effectId = 101,
            effectName = "Test Booze Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals("drink 1 test maximizer booze", result.cmd)
    }

    @Test
    fun consumptionRule_chewPassesThrough() {
        registerItem(9003, "test maximizer spleen", ItemPrimaryUse.SPLEEN)
        registerConsumable("test maximizer spleen", ConsumableType.SPLEEN, 1)
        val ctx = ruleContext(
            source = "chew 1 test maximizer spleen",
            effectId = 102,
            effectName = "Test Spleen Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals("chew 1 test maximizer spleen", result.cmd)
    }

    @Test
    fun consumptionRule_skipsNonGItemInGLover() {
        registerItem(9004, "chrome sword", ItemPrimaryUse.WEAPON)
        val ctx = ruleContext(
            source = "use 1 chrome sword",
            effectId = 103,
            effectName = "Some Effect",
            charState = CharacterState(
                level = 15,
                challengePath = "G-Lover",
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_allowsCafeFoodInGLover() {
        registerItem(9005, "Peche a la Frog", ItemPrimaryUse.FOOD)
        CafeDatabase.injectForTest(9005, "Peche a la Frog", ConsumableType.FOOD)
        registerConsumable("Peche a la Frog", ConsumableType.FOOD, 2)
        val ctx = ruleContext(
            source = "eat 1 Peche a la Frog",
            effectId = 104,
            effectName = "Cafe Food Effect",
            charState = CharacterState(
                level = 15,
                challengePath = "G-Lover",
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
    }

    @Test
    fun consumptionRule_skipsDietingPill() {
        registerItem(ItemDatabase.DIETING_PILL, "dieting pill", ItemPrimaryUse.SPLEEN)
        registerConsumable("dieting pill", ConsumableType.SPLEEN, 1)
        val ctx = ruleContext(
            source = "chew 1 dieting pill",
            effectId = 105,
            effectName = "Dieting",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_skipsBoxOfSunshineInRonin() {
        registerItem(ItemDatabase.BOX_OF_SUNSHINE, "box of sunshine", ItemPrimaryUse.MULTIPLE)
        ModifierDatabase.injectForTest(
            "Item",
            "box of sunshine",
            """Effect: "The Smile of Mr. A.", Effect Duration: 40""",
        )
        val ctx = ruleContext(
            source = "use 1 box of sunshine",
            effectId = 48,
            effectName = "The Smile of Mr. A.",
            charState = CharacterState(level = 15, roninLeft = 5),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_skipsTriviaMasterWithoutCards() {
        val ctx = ruleContext(
            source = "use 1 Trivial Avocations Card: What?, 1 Trivial Avocations Card: When?",
            effectId = 106,
            effectName = "Trivia Master",
            charState = CharacterState(level = 15, isHardcore = true),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_skipsVintnerWineWrongEffect() {
        registerItem(ItemDatabase.VAMPIRE_VINTNER_WINE, "1950 Vampire Vintner wine", ItemPrimaryUse.DRINK)
        val prefs = Preferences(MapSettings())
        prefs.setString("vintnerWineEffect", "Wine-Hot")
        val ctx = ruleContext(
            source = "drink 1 1950 Vampire Vintner wine",
            effectId = 107,
            effectName = "Wine-Cold",
            preferences = prefs,
            inventoryCount = { id -> if (id == ItemDatabase.VAMPIRE_VINTNER_WINE) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_allowsVintnerWineMatchingEffect() {
        registerItem(ItemDatabase.VAMPIRE_VINTNER_WINE, "1950 Vampire Vintner wine", ItemPrimaryUse.DRINK)
        registerConsumable("1950 Vampire Vintner wine", ConsumableType.DRINK, 1)
        val prefs = Preferences(MapSettings())
        prefs.setString("vintnerWineEffect", "Wine-Hot")
        val ctx = ruleContext(
            source = "drink 1 1950 Vampire Vintner wine",
            effectId = 108,
            effectName = "Wine-Hot",
            preferences = prefs,
            inventoryCount = { id -> if (id == ItemDatabase.VAMPIRE_VINTNER_WINE) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals(12, result.duration)
    }

    @Test
    fun consumptionRule_skipsWhenFullnessExceeded() {
        registerItem(9006, "heavy lunch", ItemPrimaryUse.FOOD)
        registerConsumable("heavy lunch", ConsumableType.FOOD, 5)
        val ctx = ruleContext(
            source = "eat 1 heavy lunch",
            effectId = 109,
            effectName = "Heavy Lunch Effect",
            charState = CharacterState(
                level = 15,
                fullness = 12,
                fullnessLimit = 15,
            ),
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_includeAllUnknownItem() {
        val ctx = ruleContext(
            source = "eat 1 unknown maximizer food",
            effectId = 110,
            effectName = "Unknown Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("(identify & eat 1 unknown maximizer food)", result.text)
    }

    @Test
    fun consumptionRule_resolvesBangPotionViaPref() {
        registerItem(819, "generic bang potion", ItemPrimaryUse.POTION)
        val prefs = Preferences(MapSettings()).apply {
            setString("lastBangPotion819", "explosiveness")
        }
        val ctx = ruleContext(
            source = "use 1 potion of explosiveness",
            effectId = 113,
            effectName = "Bang Potion Effect",
            preferences = prefs,
            inventoryCount = { id -> if (id == 819) 1 else 0 },
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals("use 1 potion of explosiveness", result.cmd)
    }

    @Test
    fun consumptionRule_unknownBangPotionWithoutPref_skipsWhenIncludeAllFalse() {
        val ctx = ruleContext(
            source = "use 1 potion of unknown effect",
            effectId = 114,
            effectName = "Unknown Bang Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(true, result.skip)
    }

    @Test
    fun consumptionRule_unknownBangPotionWithoutPref_includeAllHintUnchanged() {
        val ctx = ruleContext(
            source = "use 1 potion of unknown effect",
            effectId = 115,
            effectName = "Unknown Bang Effect",
            includeAll = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals("", result.cmd)
        assertEquals("(identify & use 1 potion of unknown effect)", result.text)
    }

    @Test
    fun consumptionRule_itemEffectDuration() {
        registerItem(9007, "duration test potion", ItemPrimaryUse.POTION)
        ModifierDatabase.injectForTest(
            "Item",
            "duration test potion",
            """Effect: "Duration Test Effect", Effect Duration: 25""",
        )
        val ctx = ruleContext(
            source = "use 1 duration test potion",
            effectId = 111,
            effectName = "Duration Test Effect",
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertEquals(false, result.skip)
        assertEquals(25, result.duration)
    }

    @Test
    fun consumptionRule_hasEffectAppendsRemovalSuffix() {
        registerItem(9008, "removal test food", ItemPrimaryUse.FOOD)
        registerConsumable("removal test food", ConsumableType.FOOD, 1)
        val ctx = ruleContext(
            source = "eat 1 removal test food",
            effectId = 112,
            effectName = "Buff To Remove",
            hasEffect = true,
        )
        val result = MaximizerBoostSourceRules.apply(ctx)
        assertNotNull(result)
        assertTrue(result.text.orEmpty().contains("(to remove Buff To Remove)"))
    }

    private fun registerConsumable(name: String, type: ConsumableType, amount: Int) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = type,
                amount = amount,
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

    private fun sweetSynthesisSkillManager(): SkillManager {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 166,
                name = "Sweet Synthesis",
                image = "pep_patty.gif",
                tags = setOf("nc", "effect", "self"),
                mpCost = 0,
                duration = 30,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        val client = HttpClient(MockEngine { _ -> respond("{}", HttpStatusCode.OK) })
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skills.learnLocalSkill(
            SkillData(
                id = 166,
                name = "Sweet Synthesis",
                type = SkillType.NONCOMBAT,
                mpCost = 0,
                dailyLimit = 0,
                timesCast = 0,
            ),
        )
        return skills
    }

    private fun registerCandy(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.POTION,
                secondaryUses = setOf("candy1"),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun makeFamiliarManager(state: FamiliarState): FamiliarManager {
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(state)
        return fm
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
        familiarManager: FamiliarManager? = null,
        charState: CharacterState? = null,
        includeAll: Boolean = false,
        activeEffects: List<EffectData> = emptyList(),
        hasEffect: Boolean = false,
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
                activeEffects = activeEffects,
                inventory = MaximizerEmitSlot.InventorySnapshot(),
                inventoryCount = inventoryCount,
                gameDatabase = stubDb,
                preferences = preferences,
                mallPriceManager = null,
                priceLevel = MaximizerPriceLevel.DONT_CHECK,
                skillManager = skillManager,
                familiarManager = familiarManager,
                includeAll = includeAll,
            ),
            effectId = effectId,
            effectName = effectName,
            source = source,
            hasEffect = hasEffect,
        )
    }

    private fun registerItem(
        id: Int,
        name: String,
        primaryUse: ItemPrimaryUse,
        secondaryUses: Set<String> = emptySet(),
    ) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = secondaryUses,
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
