package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.UseItemConsumptionSync
import net.sourceforge.kolmafia.session.ConsumptionHelperState
import net.sourceforge.kolmafia.session.GreyYouManager
import net.sourceforge.kolmafia.session.MonsterManuelManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryPhase4470Test {

    @BeforeTest
    fun setUp() {
        ConsumptionHelperState.resetForTest()
        UseItemConsumptionSync.setLastUpdateForTest("")
        KolGameHolidayCalendar.calendarDayOverride = 8 // muscle day
        GreyYouManager.resetForTest()
        MonsterManuelManager.flushCache()
    }

    @AfterTest
    fun tearDown() {
        ConsumptionHelperState.resetForTest()
        UseItemConsumptionSync.setLastUpdateForTest("")
        KolGameHolidayCalendar.calendarDayOverride = null
        GreyYouManager.resetForTest()
        MonsterManuelManager.flushCache()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun stubAdventureManager(
        preferences: Preferences = prefs(),
        character: KoLCharacter = KoLCharacter(),
    ): AdventureManager = AdventureManager(
        adventureRequest = AdventureRequest(HttpClient(MockEngine { respond("") })),
        fightRequest = FightRequest(HttpClient(MockEngine { respond("") })),
        choiceRequest = ChoiceRequest(HttpClient(MockEngine { respond("") })),
        characterRequest = CharacterRequest(HttpClient(MockEngine { respond("") })),
        character = character,
        preferences = preferences,
        eventBus = GameEventBus(),
    )

    @Test
    fun canWalkFromChoice_defaultsTrueOutsideChoice() {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, "print(to_string(can_walk_from_choice()));"))
    }

    @Test
    fun canWalkFromChoice_falseWhenBlockedChoiceActive() {
        val preferences = prefs()
        preferences.setInt(AdventureManager.LAST_CHOICE_ID, 99999)
        val mgr = stubAdventureManager(preferences)
        mgr.testSetChoiceResolution(true)
        val lib = GameRuntimeLibrary(preferences = preferences, adventureManager = mgr)
        assertEquals("false", outputLib(lib, "print(to_string(can_walk_from_choice()));"))
    }

    @Test
    fun currentRadSickness_readsCharacterState() {
        val character = KoLCharacter()
        character.updateFromApiResponse(CharacterApiResponse(radsickness = "17"))
        val lib = GameRuntimeLibrary(character = character)
        assertEquals("17", outputLib(lib, "print(current_rad_sickness());"))
    }

    @Test
    fun leetify_andStatBonusToday() {
        val lib = GameRuntimeLibrary()
        assertEquals("1337", outputLib(lib, """print(leetify("leet"));"""))
        assertEquals("muscle", outputLib(lib, "print(stat_bonus_today());"))
    }

    @Test
    fun everyCardName_uniqueMatch() {
        val lib = GameRuntimeLibrary()
        assertEquals("Gift Card", outputLib(lib, """print(every_card_name("gift"));"""))
        assertEquals("", outputLib(lib, """print(every_card_name("zzzzz"));"""))
    }

    @Test
    fun clearHelpers_andLastItemMessage() {
        UseItemConsumptionSync.setLastUpdateForTest("too full")
        val lib = GameRuntimeLibrary()
        assertEquals("too full", outputLib(lib, "print(last_item_message());"))
        outputLib(lib, "clear_food_helper(); clear_booze_helper();")
    }

    @Test
    fun dartSkillsToParts_parsesPref() {
        val preferences = prefs()
        preferences.setString("_currentDartboard", "7503:head,7504:torso")
        val lib = GameRuntimeLibrary(preferences = preferences)
        assertEquals("2", outputLib(lib, "print(count(dart_skills_to_parts()));"))
    }

    @Test
    fun monsterFactoidsAvailable_cached() {
        val monster = MonsterDatabase.getById(1) ?: return
        MonsterManuelManager.registerMonster(
            monster.id,
            """<ul><li>fact one<li>fact two</ul>""",
        )
        val lib = GameRuntimeLibrary()
        assertEquals(
            "2",
            outputLib(lib, """print(monster_factoids_available(to_monster("${monster.name}"), true));"""),
        )
    }

    @Test
    fun absorbedMonsters_mapAndPickpocketOfflineBuffer() {
        val monsterId = MonsterDatabase.byId.keys.firstOrNull { it > 0 } ?: 210
        GreyYouManager.absorbedMonsters += monsterId
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, "print(count(absorbed_monsters()));"))
        assertEquals("steal", outputLib(lib, "print(pickpocket());"))
    }

    @Test
    fun reverseNumberology_returnsMap() {
        val character = KoLCharacter()
        character.updateFromApiResponse(CharacterApiResponse(level = "10"))
        val lib = GameRuntimeLibrary(character = character)
        assertTrue(outputLib(lib, "print(count(reverse_numberology()));").toInt() > 0)
    }

    @Test
    fun preValidateAdventure_springBreakNeedsAirport() {
        val lib = GameRuntimeLibrary(character = KoLCharacter())
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(pre_validate_adventure(to_location("Spring Break Beach"))));""",
            ),
        )
    }

    @Test
    fun revisionIsPhase4470() {
        val lib = GameRuntimeLibrary()
        assertEquals("phase4490", outputLib(lib, "print(get_revision());"))
        assertEquals("phase4490", GameRuntimeLibrary.REVISION)
    }
}
