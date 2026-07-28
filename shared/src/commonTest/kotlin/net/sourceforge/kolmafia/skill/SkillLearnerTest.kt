package net.sourceforge.kolmafia.skill

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BirdOfTheDaySync

class SkillLearnerTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun registerSeekBirdSkill() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID,
                name = BirdOfTheDaySync.SEEK_OUT_A_BIRD_BASE_NAME,
                image = "findbird.gif",
                tags = setOf("nc", "effect", "self"),
                mpCost = 5,
                duration = 10,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
    }

    private fun registerTimberwolf() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF,
                name = "Snarl of the Timberwolf",
                image = "wolfmask.gif",
                tags = setOf("nc", "effect", "self"),
                mpCost = 10,
                duration = 10,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
    }

    private fun registerBearEssence() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = BattleLearnSkillIds.BEAR_ESSENCE,
                name = "Bear Essence",
                image = "stench.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    private fun fakeSkillManager(): SkillManager {
        val client = HttpClient(MockEngine { respond("") })
        return SkillManager(client, SkillCastRequest(client), GameEventBus())
    }

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()

        override fun consumeItemLocally(itemId: Int, quantity: Int) {
            if (quantity <= 0) return
            val current = flow.value
            val item = current.items[itemId] ?: return
            val remaining = item.quantity - quantity
            val updated = current.items.toMutableMap()
            if (remaining <= 0) {
                updated.remove(itemId)
            } else {
                updated[itemId] = item.copy(quantity = remaining)
            }
            flow.value = current.copy(items = updated)
        }
    }

    @Test
    fun learnSkill_firstLearn_setsPrefAndAddsToSkillManager() {
        registerSeekBirdSkill()
        val p = prefs()
        val manager = fakeSkillManager()
        assertEquals(
            BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID,
            SkillLearner.learnSkill(
                BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID,
                p,
                manager,
                mpCostOverride = 20,
                firstLearnOnly = true,
            ),
        )
        assertEquals(1, p.getInt("skillLevel7323", 0))
        val learned = manager.state.value.skills.single()
        assertEquals(BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID, learned.id)
        assertEquals(BirdOfTheDaySync.SEEK_OUT_A_BIRD_BASE_NAME, learned.name)
        assertEquals(20, learned.mpCost)
        assertEquals(SkillType.NONCOMBAT, learned.type)
    }

    @Test
    fun learnSkill_secondLearn_isNoOp() {
        registerSeekBirdSkill()
        val p = prefs()
        p.setInt("skillLevel7323", 1)
        val manager = fakeSkillManager()
        assertEquals(
            0,
            SkillLearner.learnSkill(
                BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID,
                p,
                manager,
                firstLearnOnly = true,
            ),
        )
        assertTrue(manager.state.value.skills.isEmpty())
    }

    @Test
    fun learnSkill_timberwolf_consumesWolfStandard() {
        registerTimberwolf()
        val inv = TestInventoryManager(
            mapOf(
                BattleLearnSkillIds.TATTERED_WOLF_STANDARD to InventoryItem(
                    BattleLearnSkillIds.TATTERED_WOLF_STANDARD,
                    "tattered wolf standard",
                    1,
                    ItemType.OTHER,
                ),
            ),
        )
        val manager = fakeSkillManager()
        assertEquals(
            BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF,
            SkillLearner.learnSkill(
                BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF,
                prefs(),
                manager,
                inventoryManager = inv,
            ),
        )
        assertFalse(
            inv.state.value.items.containsKey(BattleLearnSkillIds.TATTERED_WOLF_STANDARD),
        )
        assertEquals(
            BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF,
            manager.state.value.skills.single().id,
        )
    }

    @Test
    fun learnSkill_bearEssence_incrementsPref() {
        registerBearEssence()
        val p = prefs()
        val manager = fakeSkillManager()
        SkillLearner.learnSkill(BattleLearnSkillIds.BEAR_ESSENCE, p, manager)
        SkillLearner.learnSkill(BattleLearnSkillIds.BEAR_ESSENCE, p, manager)
        assertEquals(2, p.getInt("skillLevel134", 0))
    }
}
