package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectData as ActiveEffectData
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerReplaceableMutexTest {
    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        EffectDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        UneffectSkillEffectMap.rebuild()
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        EffectDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        UneffectSkillEffectMap.rebuild()
    }

    @Test
    fun applyEffectGain_removesReplaceablePeerBeforeAdding() {
        setupReplaceableEffects(
            "replaceable song a" to "Meat Drop: +5",
            "replaceable song b" to "Meat Drop: +10",
        )
        val active = listOf(
            ActiveEffectData(id = 9100, name = "replaceable song a", duration = 5),
        )
        val overlay = ReplaceableEffectMutex.applyEffectGain(
            active,
            ActiveEffectData(id = 9101, name = "replaceable song b", duration = 1),
        )
        assertEquals(1, overlay.size)
        assertEquals(9101, overlay.single().id)
    }

    @Test
    fun buildEffectBoosts_includesGainWhenReplaceablePeerActive() {
        setupReplaceableEffects(
            "replaceable song a" to "Meat Drop: +5",
            "replaceable song b" to "Meat Drop: +10",
        )
        val ctx = nonEquipmentContext(
            activeEffects = listOf(
                ActiveEffectData(id = 9100, name = "replaceable song a", duration = 5),
            ),
            skillManager = skillManagerFor("replaceable song b", 9101),
        )
        val boosts = MaximizerNonEquipmentBoosts.build(ctx)
        assertTrue(boosts.any { it.text.contains("replaceable song b", ignoreCase = true) })
    }

    private fun setupReplaceableEffects(vararg effects: Pair<String, String>) {
        effects.forEachIndexed { index, (name, modifiers) ->
            val id = 9100 + index
            ModifierDatabase.injectForTest("Effect", name, modifiers)
            SkillDefinitionDatabase.registerForTest(
                SkillDefinition(
                    id = id,
                    name = name,
                    image = "song.gif",
                    tags = setOf("nc", "effect"),
                    mpCost = 10,
                    duration = 5,
                    isPassive = false,
                    isCombat = false,
                    isNonCombat = true,
                    isSong = true,
                ),
            )
            EffectDatabase.registerForTest(
                EffectData(
                    id = id,
                    name = name,
                    image = "song.gif",
                    descId = "song$index",
                    quality = EffectQuality.NEUTRAL,
                    attributes = emptySet(),
                    actions = "cast 1 $name",
                ),
            )
        }
        UneffectSkillEffectMap.rebuild()
        ModifierDatabase.injectForTest(
            "MutexER",
            effects.joinToString("/") { it.first },
            "none",
        )
        ModifierDatabase.rebuildReplaceableMutexEffectsForTest()
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
        activeEffects: List<ActiveEffectData> = emptyList(),
        skillManager: SkillManager? = null,
    ): MaximizerNonEquipmentBoosts.Context {
        val spec = MaximizeGoal.parseSpec("meat")!!
        val plan = MaximizerEmitSlot.Plan(
            goal = "meat",
            spec = spec,
            scoreBefore = 0.0,
            scoreAfter = 10.0,
            bestPerSlot = emptyMap(),
        )
        return MaximizerNonEquipmentBoosts.Context(
            plan = plan,
            charState = CharacterState(level = 15),
            activeEffects = activeEffects,
            inventory = MaximizerEmitSlot.InventorySnapshot(),
            inventoryCount = { 0 },
            gameDatabase = GameDatabase(),
            preferences = Preferences(MapSettings()),
            mallPriceManager = null,
            priceLevel = MaximizerPriceLevel.DONT_CHECK,
            skillManager = skillManager,
        )
    }
}
