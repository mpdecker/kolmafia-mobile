package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryLocationTest {

    private val kitchenZone = AdventureZone(
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

    @BeforeTest
    fun setUp() {
        AdventureDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        AdventureDatabase.resetForTest()
    }

    @Test
    fun myLocation_readsLastLocationPref() {
        val p = prefs()
        p.setString(Preferences.LAST_LOCATION, "The Spooky Forest")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("The Spooky Forest", outputLib(lib, "print(my_location());"))
    }

    @Test
    fun myId_readsPlayerIdFromCharacter() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(playerid = "424242"))
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("424242", outputLib(lib, "print(to_string(my_id()));"))
    }

    @Test
    fun locationName_resolvesSnarfblatViaAdventureDatabase() {
        AdventureDatabase.injectForTest(kitchenZone)
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(
            "The Haunted Kitchen",
            outputLib(lib, """print(location_name(to_location("388")));"""),
        )
    }

    @Test
    fun locationName_resolvesLocationDatabaseSnarfblat() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(
            "The Haunted Pantry",
            outputLib(lib, """print(location_name("18"));"""),
        )
    }

    @Test
    fun locationAvailable_falseWhenStatTooLow() {
        AdventureDatabase.injectForTest(kitchenZone)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "1", buffedmus = "5"),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(location_available(to_location("The Haunted Kitchen"))));"""),
        )
    }

    @Test
    fun locationAvailable_trueWhenStatRequirementMet() {
        AdventureDatabase.injectForTest(kitchenZone)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "1", buffedmus = "25"),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(location_available(to_location("388"))));"""),
        )
    }

    @Test
    fun locationAvailable_matchesAdventurePrepZoneGates() {
        AdventureDatabase.injectForTest(kitchenZone)
        val cs = CharacterState(buffedMusc = 5, characterClass = 1)
        assertFalse(AdventurePrep.canAdventureAtZone("The Haunted Kitchen", cs, kitchenZone))
        val lib = GameRuntimeLibrary(character = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "1", buffedmus = "5"))
        })
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(location_available("The Haunted Kitchen")));"""),
        )
    }
}
