package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.data.AdventureQueueDatabase
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.ZoneCombatCalculator
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.AvailableCombatSkills
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * Focused XLVI Track B coverage (phases 6631–6650).
 * Revision bump deferred to parent (phase6670); stays phase6670.
 */
class GameRuntimeLibraryPhase6650Test {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
        AvailableCombatSkills.clear()
        AdventureQueueDatabase.resetQueue()
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
        AvailableCombatSkills.clear()
        AdventureQueueDatabase.resetQueue()
    }

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun revision_staysPhase6610() {
        assertEquals("phase6670", GameRuntimeLibrary.REVISION)
        assertEquals("6670", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun appearance_rates_checkZonesAndConditionalBossBat() = runBlocking {
        CombatDatabase.load()
        AdventureQueueDatabase.checkZones()
        val rates = ZoneCombatCalculator.appearanceRates(
            locationName = "The Boss Bat's Lair",
            includeQueue = false,
            ctx = ZoneCombatCalculator.Context(),
        )
        // Conditional: Boss Bat weight 0 until turns > 3 — omitted or zero (not positive).
        val boss = rates.entries.firstOrNull { it.key.equals("Boss Bat", ignoreCase = true) }?.value ?: 0.0
        assertTrue(boss <= 0.0, "Boss Bat should not have a positive rate yet, got $boss")
        assertTrue(rates.containsKey(""))
    }

    @Test
    fun get_location_monsters_rosterIncludesZeroWeightExcludesUltraRare() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"))[to_monster("spooky vampire")]));""",
            ).trim(),
        )
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"))[to_monster("Baiowulf")]));""",
            ).trim(),
        )
        // weight 0 roster residual (desktop includes; prior mobile rate>0 hid these)
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"))[to_monster("The Headless Horseman")]));""",
            ).trim(),
        )
    }

    @Test
    fun get_location_monsters_includeQueueExcludesBanished() = runBlocking {
        CombatDatabase.load()
        val p = prefs()
        val banishes = BanishManager(p)
        banishes.banishMonster("spooky mummy", Banisher.ICE_HOUSE, currentTurn = 0)
        val lib = GameRuntimeLibrary(preferences = p, banishManager = banishes)
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"), true)[to_monster("spooky mummy")]));""",
            ).trim(),
        )
        // Without includeQueue, roster still lists the monster
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(get_location_monsters(to_location("The Spooky Forest"), false)[to_monster("spooky mummy")]));""",
            ).trim(),
        )
    }

    @Test
    fun combat_skill_available_dropdownOnly() {
        AvailableCombatSkills.clear()
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 1003,
                name = "Entangling Noodles",
                image = "x.gif",
                tags = emptySet(),
                mpCost = 3,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
        assertEquals(
            "false",
            outputLib(GameRuntimeLibrary(), """print(combat_skill_available(to_skill("Entangling Noodles")));""")
                .trim().lowercase(),
        )
        AvailableCombatSkills.setFromFightHtml(
            """
            <select name=whichskill>
            <option value="1003">Entangling Noodles (3 MP)</option>
            </select>
            """.trimIndent(),
            preferences = prefs(),
        )
        assertTrue(AvailableCombatSkills.has(1003))
        assertEquals(
            "true",
            outputLib(GameRuntimeLibrary(), """print(combat_skill_available(to_skill("Entangling Noodles")));""")
                .trim().lowercase(),
        )
        AvailableCombatSkills.clear()
    }

    @Test
    fun combat_skill_available_preservesSetWithoutSelect() {
        AvailableCombatSkills.add(15)
        AvailableCombatSkills.setFromFightHtml("<html>no dropdown</html>")
        assertTrue(AvailableCombatSkills.has(15))
        AvailableCombatSkills.clear()
    }

    @Test
    fun combat_skill_available_lovebugUnlockPref() {
        val p = prefs()
        AvailableCombatSkills.setFromFightHtml(
            """
            <select name=whichskill>
            <option value="7245">Open a Big Yellow Present (1 MP)</option>
            </select>
            """.trimIndent(),
            preferences = p,
        )
        assertTrue(p.getBoolean("lovebugsUnlocked", false))
        AvailableCombatSkills.clear()
    }

    @Test
    fun fightActions_offlineReturnActionToken() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("attack", outputLib(lib, "print(attack());").trim())
        assertEquals("steal", outputLib(lib, "print(steal());").trim())
        assertEquals("twiddle", outputLib(lib, "print(twiddle());").trim())
        assertEquals("runaway", outputLib(lib, "print(runaway());").trim())
        assertEquals("pickpocket", outputLib(lib, "print(pickpocket());").trim())
    }

    @Test
    fun greyGooseSkills_skippedWhenUnderweight() {
        AvailableCombatSkills.setFromFightHtml(
            """
            <select name=whichskill>
            <option value="7408">Emit Matter Duplicating Drones (0 MP)</option>
            <option value="15">Tongue of the Walrus (10 MP)</option>
            </select>
            """.trimIndent(),
            familiarWeight = 5,
        )
        assertFalse(AvailableCombatSkills.has(7408))
        assertTrue(AvailableCombatSkills.has(15))
        AvailableCombatSkills.clear()
    }
}
