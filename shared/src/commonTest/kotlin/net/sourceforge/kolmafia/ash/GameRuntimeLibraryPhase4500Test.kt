package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.ColdMedicineCabinetGuess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.CupOf13sDatabase
import net.sourceforge.kolmafia.data.EquipmentData
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.HeartstoneDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AccountSync

class GameRuntimeLibraryPhase4500Test {

    private fun mockClient() = HttpClient(MockEngine) {
        engine {
            addHandler { respond("", HttpStatusCode.OK) }
        }
    }

    @Test
    fun revision_phase4500_batchStillRegistered() {
        assertEquals("phase5890", GameRuntimeLibrary.REVISION)
        assertTrue(outputLib(GameRuntimeLibrary(), "print(get_revision());").contains("phase"))
    }

    @Test
    fun my_effects_mapsDurationsIncludingIntrinsic() {
        val effects = EffectManager(mockClient(), GameEventBus())
        effects.replaceEffectsForTest(
            listOf(
                EffectData(id = 1, name = "Goofball Withdrawal", duration = 5),
                EffectData(id = 2, name = "Intrinsic Spiciness", duration = Int.MAX_VALUE),
            ),
        )
        val lib = GameRuntimeLibrary(effectManager = effects, preferences = Preferences(MapSettings()))
        assertEquals("2", outputLib(lib, "print(count(my_effects()));"))
        val out = outputLib(
            lib,
            """
            foreach ef, turns in my_effects() {
              print(to_string(ef) + "=" + turns);
            }
            """.trimIndent(),
        )
        assertTrue(out.contains("Goofball Withdrawal=5"))
        assertTrue(out.contains("Intrinsic Spiciness=-1"))
    }

    @Test
    fun get_title_and_avatar() {
        val character = KoLCharacter().apply {
            setTitle("the Humble")
            setAvatar("otherimages/classav1a.gif")
        }
        val lib = GameRuntimeLibrary(character = character, preferences = Preferences(MapSettings()))
        assertEquals("the Humble", outputLib(lib, "print(get_title());"))
        assertEquals("1", outputLib(lib, "print(count(get_avatar()));"))
        assertEquals(
            "otherimages/classav1a.gif",
            outputLib(lib, "print(get_avatar()[0]);"),
        )
    }

    @Test
    fun math_and_string_utils() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals("3", outputLib(lib, "print(truncate(3.9));"))
        assertEquals("4.0", outputLib(lib, "print(square_root(16.0));"))
        assertEquals("5", outputLib(lib, """print(last_index_of("banana", "a"));"""))
        assertTrue(outputLib(lib, "print(log_n(2.718281828));").toDouble() in 0.9..1.1)
    }

    @Test
    fun buffer_insert_delete_set_length() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(
            lib,
            """
            buffer b = to_buffer("abcd");
            insert(b, 2, "X");
            delete(b, 1, 3);
            set_length(b, 2);
            print(buffer_to_string(b));
            """.trimIndent(),
        )
        assertEquals("ac", out.trim())
    }

    @Test
    fun matcher_append_and_group_names() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(
            lib,
            """
            matcher m = create_matcher("(?<word>\\w+)", "aa bb");
            buffer b;
            find(m);
            append_replacement(m, b, "X");
            find(m);
            append_replacement(m, b, "Y");
            append_tail(m, b);
            print(buffer_to_string(b));
            print(count(group_names(m)));
            """.trimIndent(),
        )
        val lines = out.trim().lines()
        assertEquals("X Y", lines[0])
        assertEquals("1", lines[1])
    }

    @Test
    fun ignore_zone_warnings_fromAccountSync() {
        val character = KoLCharacter()
        val prefs = Preferences(MapSettings())
        AccountSync.parseOptionTab(
            """<input checked="checked" name="flag_ignorezonewarnings" />""",
            prefs,
            character,
        )
        val lib = GameRuntimeLibrary(character = character, preferences = prefs)
        assertEquals("true", outputLib(lib, "print(get_ignore_zone_warnings());"))
    }

    @Test
    fun shield_dr_parsesEquipment() {
        EquipmentDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(
                id = 1926,
                name = "barskin buckler",
                descId = "d",
                image = "i",
                primaryUse = ItemPrimaryUse.OFFHAND,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(
            1926,
            EquipmentData("barskin buckler", 25, null, 0, "shield", "1"),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals(
                "1",
                outputLib(lib, """item it = to_item("barskin buckler"); print(shield_dr(it));"""),
            )
        } finally {
            EquipmentDatabase.resetForTest()
        }
    }

    @Test
    fun heartstone_and_cup_of_13s() {
        assertEquals("B", HeartstoneDatabase.middleLetter("a b a")?.letter)
        assertEquals(3, HeartstoneDatabase.spaceStrippedStringLength("a b c"))
        CupOf13sDatabase.registerForTest(1, 7)
        try {
            ItemDatabase.registerForTest(
                ItemData(
                    id = 1,
                    name = "seal-clubbing club",
                    descId = "d",
                    image = "i",
                    primaryUse = ItemPrimaryUse.WEAPON,
                    secondaryUses = emptySet(),
                    access = setOf('t'),
                    autosellPrice = 1,
                    plural = null,
                ),
            )
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals("B", outputLib(lib, """print(heartstone_middle_letter("a b a"));"""))
            assertEquals("3", outputLib(lib, """print(heartstone_string_length("a b c"));"""))
            assertEquals(
                "7",
                outputLib(
                    lib,
                    """item it = to_item("seal-clubbing club"); print(cup_of_13s_tier(it));""",
                ),
            )
        } finally {
            CupOf13sDatabase.resetForTest()
        }
    }

    @Test
    fun expected_cold_medicine_cabinet_guessesEquipment() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_coldMedicineEquipmentTaken", 0)
        prefs.setString("lastCombatEnvironments", "iiiiiiiiiii")
        assertEquals(10816, ColdMedicineCabinetGuess.guessNextEquipment(prefs))
        assertEquals(10829, ColdMedicineCabinetGuess.guessNextPill(prefs))
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals(
            "5",
            outputLib(lib, "print(count(expected_cold_medicine_cabinet()));"),
        )
    }
}
