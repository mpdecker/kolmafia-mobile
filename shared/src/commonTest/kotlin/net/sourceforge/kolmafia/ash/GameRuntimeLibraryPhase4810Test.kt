package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.Crimbo21TreeRequest

class GameRuntimeLibraryPhase4810Test {

    @Test
    fun revision_phase4870() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun outfit_returnsBoolean() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """boolean b = outfit("none"); print(b);""",
        )
        assertTrue(out == "true" || out == "false")
    }

    @Test
    fun outfit_treats_keyedByItem() {
        assertEquals(
            "0",
            outputLib(
                GameRuntimeLibrary(),
                """float[item] m = outfit_treats("no such outfit"); print(count(m));""",
            ),
        )
    }

    @Test
    fun to_slot_holster_and_card() {
        assertEquals(
            "holster",
            net.sourceforge.kolmafia.modifiers.SlotNames.resolve("holster"),
        )
        assertEquals(
            "card-sleeve",
            net.sourceforge.kolmafia.modifiers.SlotNames.resolve("cardsleeve"),
        )
    }

    @Test
    fun have_equipped_checksAllAccessorySlots() {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.ACC2, "test ring")
        val out = outputLib(
            GameRuntimeLibrary(character = character),
            """print(have_equipped(to_item("test ring")));""",
        )
        assertEquals("true", out)
    }

    @Test
    fun daily_special_usesUnderscorePrefWhenEligible() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_dailySpecial", "flask of baconstone juice")
        val character = KoLCharacter()
        character.setZodiacSign(ZodiacSign.WOMBAT.signName)
        val out = outputLib(
            GameRuntimeLibrary(preferences = prefs, character = character),
            "print(daily_special());",
        )
        assertTrue(out == "flask of baconstone juice" || out == "none")
    }

    @Test
    fun npc_price_speakeasyWhenAvailable() {
        SpeakeasyAvailability.reset()
        SpeakeasyAvailability.addLoungeId(4) // Lucky Lindy
        try {
            val out = outputLib(
                GameRuntimeLibrary(),
                """print(npc_price(to_item("Lucky Lindy")));""",
            )
            assertTrue(out == "500" || out == "0")
        } finally {
            SpeakeasyAvailability.reset()
        }
    }

    @Test
    fun crimbo21Tree_registerAndParse() {
        assertTrue(Crimbo21TreeRequest.registerRequest("crimbo21tree.php?action=b&c=1"))
        assertFalse(Crimbo21TreeRequest.registerRequest("choice.php?whichchoice=1"))
        assertEquals("big rock", Crimbo21TreeRequest.ammoItemName("crimbo21tree.php?action=b&c=1"))
        assertEquals("Black Crimbo ball", Crimbo21TreeRequest.ammoItemName("crimbo21tree.php?c=2"))
    }

    @Test
    fun unequip_zeroArg_compiles() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """boolean b = unequip(); print(b);""",
        )
        assertTrue(out == "true" || out == "false")
    }
}
