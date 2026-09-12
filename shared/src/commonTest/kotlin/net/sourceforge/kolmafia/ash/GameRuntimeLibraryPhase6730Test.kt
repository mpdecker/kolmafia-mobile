package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.mood.Mood
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.mood.MoodTrigger
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.AccountSync
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

/**
 * Focused XLVII Track C coverage (phases 6711–6730).
 * REVISION stays phase6850 until parent wrap.
 */
class GameRuntimeLibraryPhase6730Test {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
        CollectionCacheSync.resetRetrievedFlags()
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
        CollectionCacheSync.resetRetrievedFlags()
        ModifierDatabase.resetForTest()
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun fakeSkillManager(): SkillManager {
        val engine = MockEngine { respond("OK") }
        val client = HttpClient(engine)
        return SkillManager(client, SkillCastRequest(client), GameEventBus())
    }

    @Test
    fun revision_staysPhase6670() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals("6910", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun numeric_modifier_generatedSpec_caseInsensitiveType() {
        ModifierDatabase.overrideGenerated("_spec", "Meat Drop: +17, Single Equip")
        val lib = GameRuntimeLibrary()
        assertEquals(
            "17.0",
            outputLib(lib, """print(to_string(numeric_modifier("generated:_spec", "Meat Drop")));""")
                .trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(boolean_modifier("GENERATED:_spec", "Single Equip")));""")
                .trim().lowercase(),
        )
        assertEquals(
            "17.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Generated:_spec", to_modifier("Meat Drop"))));""",
            ).trim(),
        )
    }

    @Test
    fun get_property_globalAllowsPerUserGlobalLookup() {
        val p = prefs()
        p.setString("displayName.test", "shown", global = true)
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals(
            "shown",
            outputLib(lib, """print(get_property("displayName.test", true));""").trim(),
        )
        // 1-arg System.* reads live JVM properties (desktop System.getProperty).
        assertTrue(
            outputLib(lib, """print(get_property("System.java.version"));""").trim().isNotEmpty(),
        )
    }

    @Test
    fun remove_property_fallsThroughToGlobalWhenAbsentFromUser() {
        val p = prefs()
        p.setString("orphanGlobalKey", "gone", global = true)
        assertTrue(p.propertyExists("orphanGlobalKey", global = true))
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals(
            "gone",
            outputLib(lib, """print(remove_property("orphanGlobalKey"));""").trim(),
        )
        assertFalse(p.propertyExists("orphanGlobalKey", global = true))
    }

    @Test
    fun organLimits_pathAndClassCaps() {
        assertEquals(
            20,
            ConsumptionEligibility.stomachCapacity(
                CharacterState(challengePath = "Avatar of Boris"),
            ),
        )
        assertEquals(
            4,
            ConsumptionEligibility.liverCapacity(
                CharacterState(challengePath = "Avatar of Boris"),
            ),
        )
        assertEquals(
            0,
            ConsumptionEligibility.stomachCapacity(
                CharacterState(challengePath = "License to Adventure"),
            ),
        )
        assertEquals(
            5,
            ConsumptionEligibility.spleenCapacity(
                CharacterState(challengePath = "Actually Ed the Undying"),
            ),
        )
        assertEquals(
            10,
            ConsumptionEligibility.stomachCapacity(
                CharacterState(characterClass = 18, challengePath = "None"),
            ),
        )
        assertEquals(20, AscensionPath.AVATAR_OF_BORIS.stomachCapacity)
        assertEquals(AscensionPath.NUCLEAR_AUTUMN, AscensionPath.fromApiString("Nuclear Autumn"))
    }

    @Test
    fun spleen_limit_ashUsesPathBase() {
        val char = KoLCharacter()
        char.setChallengePath("Nuclear Autumn")
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("3", outputLib(lib, "print(spleen_limit());").trim())
        assertEquals("3", outputLib(lib, "print(fullness_limit());").trim())
        assertEquals("2", outputLib(lib, "print(inebriety_limit());").trim())
    }

    @Test
    fun mood_loadActiveMood_inheritsLibraryParents() {
        val p = prefs()
        val moodMgr = MoodManager(fakeSkillManager(), p)
        val parent = Mood(
            name = "default",
            triggers = listOf(
                MoodTrigger(1, "Goofball", 1, "The Smile of Mr. A.", 1),
            ),
        )
        val child = Mood(name = "run", triggers = emptyList(), parentNames = listOf("default"))
        moodMgr.addMoodToLibrary(parent)
        moodMgr.addMoodToLibrary(child)
        // Bare name without "extends" — inheritance residual restores library parents.
        p.setString(Preferences.ACTIVE_MOOD_NAME, "run")
        p.setString(Preferences.ACTIVE_MOOD_TRIGGERS, "")
        moodMgr.loadActiveMood()
        val effective = moodMgr.activeMood!!.effectiveTriggers(moodMgr.moodLibrary)
        assertEquals(1, effective.size)
        assertEquals("Goofball", effective[0].effectName)
    }

    @Test
    fun restore_hp_preservesOuterRecoveryActiveFlag() {
        val engine = MockEngine { respond("OK") }
        val client = HttpClient(engine)
        val inv = InventoryManager(client, GameEventBus())
        val skills = fakeSkillManager()
        val rm = RecoveryManager(inv, skills, prefs())
        rm.isRecoveryActive = true
        runBlocking {
            val ok = rm.checkpointedRecoverHp(
                amount = 1,
                charState = CharacterState(currentHp = 50, maxHp = 50),
                invState = InventoryState(),
                skillState = SkillState(),
            ) {
                Triple(
                    CharacterState(currentHp = 50, maxHp = 50),
                    InventoryState(),
                    SkillState(),
                )
            }
            assertTrue(ok)
        }
        assertTrue(rm.isRecoveryActive)
    }

    @Test
    fun eudora_accountSync_setsCurrentAndRefreshFlag() {
        val p = prefs()
        AccountSync.parseAccountData(
            "account.php?am=1&action=whichpenpal&ajax=1&value=2",
            "<html>ok</html>",
            p,
        )
        assertEquals("GameInformPowerDailyPro Magazine", p.getString("eudora", ""))
        assertEquals("GameInformPowerDailyPro Magazine", p.getString("currentEudora", ""))
        assertTrue(p.getBoolean("_eudoraNeedsStatusRefresh", false))
        assertEquals(
            "GameInformPowerDailyPro Magazine",
            outputLib(GameRuntimeLibrary(preferences = p), "print(eudora());").trim(),
        )
    }

    @Test
    fun get_free_pulls_readsCacheWhenNoStorageRequest() {
        val p = prefs()
        CollectionCacheSync.saveStorage(
            p,
            storage = mapOf(1 to 1),
            freepulls = mapOf(99 to 7),
            nopulls = emptyMap(),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 99,
                name = "free relic",
                descId = "d99",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals(
            "7",
            outputLib(lib, """print(get_free_pulls()[to_item("free relic")]);""").trim(),
        )
    }
}
