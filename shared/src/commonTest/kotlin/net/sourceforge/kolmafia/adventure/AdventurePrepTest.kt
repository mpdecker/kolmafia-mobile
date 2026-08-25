package net.sourceforge.kolmafia.adventure

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.prep.AdventureGateContext
import net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions
import net.sourceforge.kolmafia.adventure.prep.ItemIds
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CustomOutfitRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdventurePrepTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences =
        Preferences(MapSettings().apply(block))

    private val overdrunkZone = AdventureZone(
        zoneName = "Holiday",
        urlParams = "adventure=23",
        locationName = "Drunken Stupor",
        environment = "outdoor",
        diffLevel = "low",
        statRequirement = 0,
        goals = emptyList(),
        isOverdrunk = true,
        noWander = false,
    )

    @Test
    fun canAdventureAt_falseWhenNoAdventuresLeft() {
        val cs = CharacterState(adventuresLeft = 0)
        assertFalse(AdventurePrep.canAdventureAt("The Haunted Pantry", cs, overdrunkZone))
    }

    @Test
    fun canAdventureAt_overdrunkZoneRequiresInebriety() {
        val sober = CharacterState(adventuresLeft = 5, inebriety = 0)
        assertFalse(AdventurePrep.canAdventureAt("Drunken Stupor", sober, overdrunkZone))

        val drunk = CharacterState(adventuresLeft = 5, inebriety = 3)
        assertTrue(AdventurePrep.canAdventureAt("Drunken Stupor", drunk, overdrunkZone))
    }

    @Test
    fun canAdventureAt_limitModeRequiresMatchingZone() {
        val cs = CharacterState(adventuresLeft = 5, limitMode = "spelunk")
        val normalZone = AdventureZone(
            zoneName = "The Kingdom",
            urlParams = "place=town",
            locationName = "The Sleazy Back Alley",
            environment = "outdoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(AdventurePrep.canAdventureAt("The Sleazy Back Alley", cs, normalZone))

        val spelunkZone = normalZone.copy(urlParams = "place=spelunk_tower")
        assertTrue(AdventurePrep.canAdventureAt("The Sleazy Back Alley", cs, spelunkZone))
    }

    @Test
    fun canAdventureAt_falseWhenMainStatBelowRequirement() {
        val lowStat = CharacterState(adventuresLeft = 5, buffedMusc = 10, characterClass = 1, level = 10)
        val highStatZone = AdventureZone(
            zoneName = "Manor1",
            urlParams = "adventure=388",
            locationName = "The Haunted Kitchen",
            environment = "indoor",
            diffLevel = "mid",
            statRequirement = 20,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        val p = prefs { putString("questM20Necklace", "started") }
        assertFalse(AdventurePrep.canAdventureAt("The Haunted Kitchen", lowStat, highStatZone, p))

        val okStat = lowStat.copy(buffedMusc = 25)
        assertTrue(AdventurePrep.canAdventureAt("The Haunted Kitchen", okStat, highStatZone, p))
    }

    @Test
    fun canAdventureAt_trueWhenStatRequirementIsZero() {
        val cs = CharacterState(adventuresLeft = 5, buffedMusc = 1, characterClass = 1)
        val zone = AdventureZone(
            zoneName = "Manor1",
            urlParams = "adventure=113",
            locationName = "The Haunted Pantry",
            environment = "indoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertTrue(AdventurePrep.canAdventureAt("The Haunted Pantry", cs, zone))
    }

    @Test
    fun prepareForAdventure_failsWhenStatTooLow() = runBlocking {
        val zone = AdventureZone(
            zoneName = "Manor1",
            urlParams = "adventure=388",
            locationName = "The Haunted Kitchen",
            environment = "indoor",
            diffLevel = "mid",
            statRequirement = 20,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        AdventureDatabase.resetForTest()
        AdventureDatabase.injectForTest(zone)
        try {
            val cs = CharacterState(adventuresLeft = 5, buffedMusc = 5, characterClass = 1)
            assertFalse(AdventurePrep.canAdventureAtZone("The Haunted Kitchen", cs, zone))
            val ok = AdventurePrep.prepareForAdventure(
                "The Haunted Kitchen",
                outfitManager = null,
                preferences = prefs(),
                character = cs,
            )
            assertFalse(ok)
        } finally {
            AdventureDatabase.resetForTest()
        }
    }

    @Test
    fun prepareForAdventure_setsZoneFamiliarFromPref() = runBlocking {
        val switchCalls = mutableListOf<String>()
        val client = HttpClient(MockEngine { respond("") })
        val fm = object : FamiliarManager(client, GameEventBus()) {
            override suspend fun setFamiliar(name: String): Result<Unit> {
                switchCalls.add(name)
                return Result.success(Unit)
            }
        }.also {
            it.testSetState(
                FamiliarState(
                    ownedFamiliars = listOf(
                        FamiliarData(id = 1, name = "Seal", race = "Seal", weight = 1, experience = 0, kills = 0)
                    )
                )
            )
        }

        val p = prefs { putString("zoneFamiliar_The Haunted Pantry", "Seal") }
        val ok = AdventurePrep.prepareForAdventure(
            "The Haunted Pantry",
            outfitManager = null,
            preferences = p,
            familiarManager = fm,
        )
        assertTrue(ok)
        assertEquals(listOf("Seal"), switchCalls)
    }

    @Test
    fun prepareForAdventure_failsWhenFamiliarManagerMissing() = runBlocking {
        val p = prefs { putString("zoneFamiliar_The Haunted Pantry", "Seal") }
        val ok = AdventurePrep.prepareForAdventure(
            "The Haunted Pantry",
            outfitManager = null,
            preferences = p,
            familiarManager = null,
        )
        assertFalse(ok)
    }

    @Test
    fun prepareForAdventure_wearsBuiltInOutfit() = runBlocking {
        var worn: String? = null
        val client = HttpClient(MockEngine { respond("") })
        val om = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = EquipmentRequest(client),
            customOutfitRequest = CustomOutfitRequest(client),
            character = KoLCharacter(),
            gameDatabase = object : GameDatabase() {},
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun wearOutfit(name: String, postWear: ((String) -> Unit)?): Boolean {
                worn = name
                return true
            }
        }
        val ok = AdventurePrep.prepareForAdventure(
            "The Mine Office",
            outfitManager = om,
            preferences = prefs(),
        )
        assertTrue(ok)
        assertEquals("Mining Gear", worn)
    }

    private val pirateRealmSeas = AdventureZone(
        zoneName = "PirateRealm",
        urlParams = "adventure=530",
        locationName = "Sailing the PirateRealm Seas",
        environment = "outdoor",
        diffLevel = "mid",
        statRequirement = 0,
        goals = emptyList(),
        isOverdrunk = false,
        noWander = true,
    )

    private val battleIsland = AdventureZone(
        zoneName = "PirateRealm Island",
        urlParams = "adventure=531",
        locationName = "Battle Island",
        environment = "outdoor",
        diffLevel = "mid",
        statRequirement = 0,
        goals = emptyList(),
        isOverdrunk = false,
        noWander = true,
    )

    @Test
    fun canAdventureAtPirateRealm_blockedWithoutDailyAccess() {
        val prefs = prefs()
        assertFalse(
            AdventurePrep.canAdventureAtPirateRealm(
                "Sailing the PirateRealm Seas",
                "PirateRealm",
                prefs,
            ),
        )
    }

    @Test
    fun canAdventureAtPirateRealm_seasAllowedWithPrToday() {
        val prefs = prefs { putBoolean("_prToday", true) }
        assertTrue(
            AdventurePrep.canAdventureAtPirateRealm(
                "Sailing the PirateRealm Seas",
                "PirateRealm",
                prefs,
            ),
        )
    }

    @Test
    fun canAdventureAtPirateRealm_namedIslandGatedOnLastIsland() {
        val prefs = prefs {
            putBoolean("_prToday", true)
            putString("_lastPirateRealmIsland", "Crab Island")
        }
        assertFalse(
            AdventurePrep.canAdventureAtPirateRealm(
                "Battle Island",
                "PirateRealm Island",
                prefs,
            ),
        )
        prefs.setString("_lastPirateRealmIsland", "Battle Island")
        assertTrue(
            AdventurePrep.canAdventureAtPirateRealm(
                "Battle Island",
                "PirateRealm Island",
                prefs,
            ),
        )
    }

    @Test
    fun canAdventureAtZone_pirateRealmIntegrated() {
        val cs = CharacterState(adventuresLeft = 5)
        val prefs = prefs {
            putBoolean("prAlways", true)
            putString("_lastPirateRealmIsland", "Battle Island")
        }
        assertTrue(
            AdventurePrep.canAdventureAtZone(
                "Battle Island",
                cs,
                battleIsland,
                prefs,
            ),
        )
        assertTrue(
            AdventurePrep.canAdventureAtZone(
                "Sailing the PirateRealm Seas",
                cs,
                pirateRealmSeas,
                prefs { putBoolean("prAlways", true) },
            ),
        )
        assertFalse(
            AdventurePrep.canAdventureAtZone(
                "Sailing the PirateRealm Seas",
                cs,
                pirateRealmSeas,
                prefs(),
            ),
        )
    }

    @Test
    fun woodsOpen_requiresLarvaOrCitadel() {
        AdventurePrep.resetForTest()
        val prefs = prefs()
        assertFalse(AdventurePrep.woodsOpen(prefs))
        prefs.setString("questL02Larva", "started")
        assertTrue(AdventurePrep.woodsOpen(prefs))
    }

    @Test
    fun tooDrunkBlocksNormalZoneWithoutWineglass() {
        AdventurePrep.resetForTest()
        val drunk = CharacterState(adventuresLeft = 5, inebriety = 20, inebrietyLimit = 14)
        val zone = AdventureZone(
            zoneName = "Town",
            urlParams = "adventure=112",
            locationName = "The Sleazy Back Alley",
            environment = "outdoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertTrue(AdventurePrep.tooDrunkToAdventure("The Sleazy Back Alley", drunk, zone))
        assertFalse(AdventurePrep.canAdventureAt("The Sleazy Back Alley", drunk, zone))
    }

    @Test
    fun tooDrunkAllowsSpelunkyLimitMode() {
        AdventurePrep.resetForTest()
        val drunk = CharacterState(
            adventuresLeft = 5,
            inebriety = 20,
            inebrietyLimit = 14,
            limitMode = "spelunky",
        )
        val zone = AdventureZone(
            zoneName = "Spelunky",
            urlParams = "place=spelunky",
            locationName = "The Mines",
            environment = "underground",
            diffLevel = "mid",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(AdventurePrep.tooDrunkToAdventure("The Mines", drunk, zone))
    }

    @Test
    fun gingerbreadRequiresAccessPref() {
        AdventurePrep.resetForTest()
        val cs = CharacterState(adventuresLeft = 5)
        val zone = AdventureZone(
            zoneName = "Gingerbread City",
            urlParams = "adventure=477",
            locationName = "Gingerbread Civic Center",
            environment = "indoor",
            diffLevel = "mid",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(AdventurePrep.canAdventureAtZone("Gingerbread Civic Center", cs, zone, prefs()))
        assertTrue(
            AdventurePrep.canAdventureAtZone(
                "Gingerbread Civic Center",
                cs,
                zone,
                prefs { putBoolean("gingerbreadCityAvailable", true) },
            ),
        )
    }

    @Test
    fun shadowRiftGenericNeedsIngress() {
        AdventurePrep.resetForTest()
        val cs = CharacterState(adventuresLeft = 5)
        val zone = AdventureZone(
            zoneName = "Shadow Rift",
            urlParams = "adventure=567",
            locationName = "Shadow Rift",
            environment = "other",
            diffLevel = "mid",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(AdventurePrep.canAdventureAtZone("Shadow Rift", cs, zone, prefs()))
        assertTrue(
            AdventurePrep.canAdventureAtZone(
                "Shadow Rift",
                cs,
                zone,
                prefs { putString("shadowRiftIngress", "town") },
            ),
        )
    }

    @Test
    fun copperheadClubRequiresShen() {
        AdventurePrep.resetForTest()
        val cs = CharacterState(adventuresLeft = 5)
        val zone = AdventureZone(
            zoneName = "Town",
            urlParams = "adventure=383",
            locationName = "The Copperhead Club",
            environment = "indoor",
            diffLevel = "mid",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertFalse(AdventurePrep.canAdventureAtZone("The Copperhead Club", cs, zone, prefs()))
        val p = prefs { putString("questL11Shen", "started") }
        assertTrue(AdventurePrep.canAdventureAtZone("The Copperhead Club", cs, zone, p))
    }

    @Test
    fun resolveOutfitExpandsKnobHarem() {
        assertEquals(
            "Knob Goblin Harem Girl Disguise",
            net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions.resolveOutfit(
                "Cobb's Knob Harem",
                null,
            ),
        )
    }

    @Test
    fun sonarsToUse_scalesWithBatQuestStep() {
        val started = AdventureGateContext(
            preferences = prefs { putString("questL04Bat", "started") },
        )
        assertEquals(
            1,
            AdventurePrepareActions.sonarsToUse("The Batrat and Ratbat Burrow", started),
        )
        val step2 = AdventureGateContext(
            preferences = prefs { putString("questL04Bat", "step2") },
        )
        assertEquals(
            0,
            AdventurePrepareActions.sonarsToUse("The Batrat and Ratbat Burrow", step2),
        )
        assertEquals(
            1,
            AdventurePrepareActions.sonarsToUse("The Boss Bat's Lair", step2),
        )
    }

    @Test
    fun filthwormGland_mapsOrchardZones() {
        assertEquals(
            ItemIds.FILTHWORM_HATCHLING_GLAND,
            AdventurePrepareActions.needsFilthwormGland("The Filthworm Feeding Grounds"),
        )
        assertEquals(
            ItemIds.FILTHWORM_GUARD_GLAND,
            AdventurePrepareActions.needsFilthwormGland("The Filthworm Queen's Chamber"),
        )
    }
}
