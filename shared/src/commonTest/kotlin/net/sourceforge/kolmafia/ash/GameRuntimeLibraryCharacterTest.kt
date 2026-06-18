package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GameRuntimeLibraryCharacterTest {

    private fun libWith(block: CharacterApiResponse.() -> CharacterApiResponse): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse().block())
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun myClass_sealClubber() {
        val lib = libWith { copy(classId = "1") }
        assertEquals("Seal Clubber", outputLib(lib, "print(my_class());"))
    }

    @Test
    fun inRun_falseWhenKingLiberated() {
        val lib = libWith { copy(kingliberated = "1") }
        assertEquals("false", outputLib(lib, "print(to_string(in_run()));"))
    }

    @Test
    fun inRun_trueWhenActive() {
        val lib = libWith { copy(kingliberated = "0") }
        assertEquals("true", outputLib(lib, "print(to_string(in_run()));"))
    }

    @Test
    fun canInteract_falseInHardcore() {
        val lib = libWith { copy(hardcore = "1") }
        assertEquals("false", outputLib(lib, "print(to_string(can_interact()));"))
    }

    @Test
    fun canInteract_falseInRonin() {
        val lib = libWith { copy(roninleft = "5") }
        assertEquals("false", outputLib(lib, "print(to_string(can_interact()));"))
    }

    @Test
    fun mySign_returnsZodiacSign() {
        val lib = libWith { copy(sign = "Opossum") }
        assertEquals("Opossum", outputLib(lib, "print(my_sign());"))
    }

    @Test
    fun underStandard_trueWhenPathIsStandard() {
        val lib = libWith { copy(path = "Standard") }
        assertEquals("true", outputLib(lib, "print(to_string(under_standard()));"))
    }

    @Test
    fun underStandard_falseWhenPathIsNone() {
        val lib = libWith { copy(path = "None") }
        assertEquals("false", outputLib(lib, "print(to_string(under_standard()));"))
    }

    @Test
    fun underStandard_falseWhenNoCharacter() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, "print(to_string(under_standard()));"))
    }

    @Test
    fun toPath_invalidNameReturnsEmpty() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("", outputLib(lib, """print(to_string(to_path("none")));""").trim())
    }

    @Test
    fun getPath_returnsMyPath() {
        val lib = libWith { copy(path = "Standard") }
        assertEquals("Standard", outputLib(lib, """print(to_string(get_path()));""").trim())
    }

    @Test
    fun getPath_matchesMyPath() {
        val lib = libWith { copy(path = "Teetotaler") }
        assertEquals(
            outputLib(lib, """print(to_string(my_path()));""").trim(),
            outputLib(lib, """print(to_string(get_path()));""").trim(),
        )
    }

    @Test
    fun myStat_returnsBuffedMuscle() {
        val lib = libWith { copy(buffedmus = "75") }
        assertEquals("75",
            outputLib(lib, """print(to_string(my_stat(to_stat("muscle"))));"""))
    }

    @Test
    fun myBuffedstat_matchesMyStat() {
        val lib = libWith { copy(buffedmys = "42") }
        assertEquals(
            outputLib(lib, """print(to_string(my_stat(to_stat("mysticality"))));"""),
            outputLib(lib, """print(to_string(my_buffedstat(to_stat("mysticality"))));"""),
        )
    }

    @Test
    fun myDiscoball_trueWhenOwned() {
        val disco = net.sourceforge.kolmafia.familiar.FamiliarData(
            id = 87, name = "Disco", race = "Autonomous Disco Ball",
            weight = 3, experience = 0, kills = 0,
        )
        val fm = net.sourceforge.kolmafia.familiar.FamiliarManager(
            io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        )
        fm.testSetState(net.sourceforge.kolmafia.familiar.FamiliarState(ownedFamiliars = listOf(disco)))
        val lib = GameRuntimeLibrary(familiarManager = fm)
        assertEquals("true", outputLib(lib, "print(to_string(my_discoball()));"))
    }

    @Test
    fun myDiscoball_falseWhenNotOwned() {
        assertEquals("false", outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(my_discoball()));"))
    }

    @Test
    fun myRolodex_readsPreference() {
        val p = prefs()
        p.setBoolean("hasRolodex", true)
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("true", outputLib(lib, "print(to_string(my_rolodex()));"))
    }

    @Test
    fun ascensionNumber_returns42() {
        val lib = libWith { copy(ascensions = "42") }
        assertEquals("42", outputLib(lib, "print(to_string(ascension_number()));"))
    }

    @Test
    fun myThrall_emptyString() {
        assertEquals("", outputLib(GameRuntimeLibrary.forTesting(), "print(my_thrall());"))
    }

    @Test
    fun turnsleft_labeledRelayCounter() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        net.sourceforge.kolmafia.session.TurnCounter.startCounting(preferences, 100, 12, "Test Counter loc=*", "x.gif")
        val lib = GameRuntimeLibrary(
            character = net.sourceforge.kolmafia.character.KoLCharacter().also {
                it.updateFromApiResponse(
                    net.sourceforge.kolmafia.character.CharacterApiResponse(
                        currentrun = "100",
                        adventures = "40",
                    ),
                )
            },
            preferences = preferences,
        )
        assertEquals("12", outputLib(lib, """print(turnsleft("Test Counter"));"""))
    }

    @Test
    fun canAdventure_trueWhenAdventuresLeft() {
        val lib = libWith { copy(adventures = "5") }
        assertEquals("true", outputLib(lib, """print(to_string(can_adventure(to_location("The Haunted Pantry"))));"""))
    }

    @Test
    fun canAdventure_falseWhenNoAdventuresLeft() {
        val lib = libWith { copy(adventures = "0") }
        assertEquals("false", outputLib(lib, """print(to_string(can_adventure(to_location("The Haunted Pantry"))));"""))
    }

    @Test
    fun canAdventure_overdrunkZoneRequiresInebriety() {
        val sober = net.sourceforge.kolmafia.character.CharacterState(adventuresLeft = 5, inebriety = 0)
        val zone = AdventureZone(
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
        assertFalse(AdventurePrep.canAdventureAt("Drunken Stupor", sober, zone))
    }

    @Test
    fun prepareForAdventure_returnsTrue() {
        assertEquals("true", outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(prepare_for_adventure()));"))
    }

    @Test
    fun adv1_returnsFalseWhenNoAdventureManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(adv1(to_location("The Haunted Pantry"), 1)));"""))
    }

    @Test
    fun resolveLocation_spookyForest_usesSnarfblat20() {
        val lib = GameRuntimeLibrary.forTesting()
        val loc = lib.resolveLocation("The Spooky Forest")
        assertEquals("20", loc?.id)
        assertEquals("The Spooky Forest", loc?.name)
    }

    @Test
    fun resolveLocation_unknownName_returnsNull() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(null, lib.resolveLocation("Totally Fake Zone Name"))
    }

    @Test
    fun adv1_unknownLocation_returnsFalse() {
        val client = HttpClient(MockEngine { respond("") })
        val mgr = net.sourceforge.kolmafia.adventure.AdventureManager(
            adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(client),
            fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(client),
            choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(client),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(client),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            preferences = prefs(),
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
        )
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        assertEquals("false", outputLib(lib, """print(to_string(adv1(to_location("Totally Fake Zone Name"), 1)));"""))
    }

    @Test
    fun turnsleft_matchesMyAdventures() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(adventures = "9")
        )
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("9", outputLib(lib, "print(to_string(turnsleft()));"))
        assertEquals("9", outputLib(lib, "print(to_string(my_adventures()));"))
    }

    @Test
    fun myAbsorbs_returnsCharacterAbsorbs() {
        val char = KoLCharacter()
        char.updateClassResource(absorbs = 4)
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("4", outputLib(lib, "print(to_string(my_absorbs()));"))
    }

    @Test
    fun guildAvailable_trueForStandardClass() {
        val lib = libWith { copy(classId = "1") }
        assertEquals("true", outputLib(lib, "print(to_string(guild_available()));"))
    }

    @Test
    fun knollAvailable_trueForKnollSign() {
        val lib = libWith { copy(sign = "Mongoose") }
        assertEquals("true", outputLib(lib, "print(to_string(knoll_available()));"))
    }

    @Test
    fun whiteCitadelAvailable_falseWhenUnstarted() {
        val prefs = com.russhwolf.settings.MapSettings()
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(
            net.sourceforge.kolmafia.preferences.Preferences(prefs),
        )
        val lib = GameRuntimeLibrary(questDatabase = db)
        assertEquals("false", outputLib(lib, "print(to_string(white_citadel_available()));"))
    }

    @Test
    fun canadiaAvailable_trueForCanadiaSign() {
        val lib = libWith { copy(sign = "Platypus") }
        assertEquals("true", outputLib(lib, "print(to_string(canadia_available()));"))
    }

    @Test
    fun gnomadsAvailable_trueForGnomadsSign() {
        val lib = libWith { copy(sign = "Wombat") }
        assertEquals("true", outputLib(lib, "print(to_string(gnomads_available()));"))
    }

    @Test
    fun friarsAvailable_trueWhenFriarQuestFinished() {
        val prefs = com.russhwolf.settings.MapSettings()
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(
            net.sourceforge.kolmafia.preferences.Preferences(prefs),
        )
        db.setProgress(net.sourceforge.kolmafia.quest.Quest.FRIAR, QuestDatabase.FINISHED)
        val lib = GameRuntimeLibrary(
            questDatabase = db,
            character = libWith { copy(ascensions = "7") }.character,
        )
        assertEquals("true", outputLib(lib, "print(to_string(friars_available()));"))
    }

    @Test
    fun blackMarketAvailable_trueWhenMacGuffinStarted() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(preferences)
        db.setProgress(net.sourceforge.kolmafia.quest.Quest.MACGUFFIN, "step1")
        val lib = GameRuntimeLibrary(questDatabase = db, preferences = preferences)
        assertEquals("true", outputLib(lib, "print(to_string(black_market_available()));"))
    }

    @Test
    fun guildStoreAvailable_trueWhenPrefMatchesAscension() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt("lastGuildStoreOpen", 3)
        val lib = GameRuntimeLibrary(
            preferences = preferences,
            character = libWith { copy(classId = "1", ascensions = "3") }.character,
        )
        assertEquals("true", outputLib(lib, "print(to_string(guild_store_available()));"))
    }

    @Test
    fun hippyStoreAvailable_falseDuringWarStep1() {
        val prefs = com.russhwolf.settings.MapSettings()
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(
            net.sourceforge.kolmafia.preferences.Preferences(prefs),
        )
        db.setProgress(net.sourceforge.kolmafia.quest.Quest.ISLAND_WAR, "step1")
        val lib = GameRuntimeLibrary(questDatabase = db)
        assertEquals("false", outputLib(lib, "print(to_string(hippy_store_available()));"))
    }

    @Test
    fun hiddenTempleUnlocked_trueWhenPrefMatchesAscension() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt("lastTempleUnlock", 5)
        val lib = GameRuntimeLibrary(
            preferences = preferences,
            character = libWith { copy(ascensions = "5") }.character,
        )
        assertEquals("true", outputLib(lib, "print(to_string(hidden_temple_unlocked()));"))
    }
}
