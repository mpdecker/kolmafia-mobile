package net.sourceforge.kolmafia.combat

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class CustomCombatLookupTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        CombatActionManager.resetForTest()
        prefs = Preferences(MapSettings())
    }

    @Test
    fun parseDefaultAndMonsterSections() {
        val lookup = CustomCombatLookup()
        lookup.load(
            """
            [ default ]
            special action
            attack with weapon

            [ huge mosquito ]
            skill saucegeyser
            attack with weapon
            """.trimIndent(),
        )
        assertEquals("huge mosquito", lookup.getBestEncounterKey("huge mosquito"))
        assertEquals(
            "skill saucegeyser",
            lookup.getStrategy("huge mosquito")!!.getAction(lookup, 0, true),
        )
        assertEquals(
            "attack with weapon",
            lookup.getStrategy("default")!!.getAction(lookup, 1, true),
        )
    }

    @Test
    fun macroDirectivesPreserveIfHasskill() {
        val lookup = CustomCombatLookup()
        lookup.load(
            """
            [ default ]
            if hasskill saucegeyser
                skill saucegeyser
            endif
            attack with weapon
            """.trimIndent(),
        )
        val strategy = lookup.getStrategy("default")!!
        assertTrue(CombatActionManager.isMacroAction(strategy.getAction(lookup, 0, true)))
        assertEquals("if hasskill saucegeyser", strategy.getAction(lookup, 0, true))
    }

    @Test
    fun locationSectionMatchUsesLastAdventure() = runBlocking {
        MonsterDatabase.load()
        AdventureDatabase.injectForTest(
            AdventureZone(
                zoneName = "Mountain",
                urlParams = "adventure.php?snarfblat=15",
                locationName = "The Spooky Forest",
                environment = "outdoor",
                diffLevel = "low",
                statRequirement = 0,
                goals = emptyList(),
                isOverdrunk = false,
                noWander = false,
            ),
        )
        val lookup = CustomCombatLookup()
        lookup.load(
            """
            [ default ]
            attack with weapon

            [ The Spooky Forest ]
            skill entangling noodles
            attack with weapon
            """.trimIndent(),
        )
        prefs.setString("lastAdventure", "The Spooky Forest")
        val key = lookup.getBestEncounterKey("huge mosquito", prefs) {
            AdventureDatabase.getByName(it)?.zoneName
        }
        assertEquals("the spooky forest", key)
    }

    @Test
    fun combatActionManagerLoadAndRoundTrip() {
        CombatActionManager.loadFromText(
            """
            [ default ]
            abort
            attack with weapon
            """.trimIndent(),
            name = "testccs",
            preferences = prefs,
        )
        assertEquals("testccs", prefs.getString("customCombatScript"))
        assertEquals("abort", CombatActionManager.getCcsCombatAction("default", 0, true, prefs))
        assertEquals(
            "attack with weapon",
            CombatActionManager.getCcsCombatAction("default", 1, true, prefs),
        )
        assertTrue(CombatActionManager.atEndOfStrategy)
    }

    @Test
    fun encounterKeyStripsArticles() {
        assertEquals("huge mosquito", CombatActionManager.encounterKey("A huge mosquito"))
        assertEquals("ice cream truck", CombatActionManager.encounterKey("an ice cream truck"))
    }

    @Test
    fun phylumFilterSection() {
        val key = CombatEncounterKey("\$phylum[bug]")
        assertTrue(key.matches("anything", monsterPhylum = "bug"))
        assertFalse(key.matches("anything", monsterPhylum = "beast"))
    }
}
