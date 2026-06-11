package net.sourceforge.kolmafia.adventure

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
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
        val lowStat = CharacterState(adventuresLeft = 5, buffedMusc = 10, characterClass = 1)
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
        assertFalse(AdventurePrep.canAdventureAt("The Haunted Kitchen", lowStat, highStatZone))

        val okStat = lowStat.copy(buffedMusc = 25)
        assertTrue(AdventurePrep.canAdventureAt("The Haunted Kitchen", okStat, highStatZone))
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
}
