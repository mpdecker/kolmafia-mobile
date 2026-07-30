package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TelescopeSync

class GameRuntimeLibraryAshP110Test {

    @Test
    fun myMaskAndParadoxicity_readFromCharacterState() {
        val char = KoLCharacter().also {
            it.updateClassResource(currentMask = "batmask", paradoxicity = 15)
        }
        val prefs = Preferences(MapSettings())
        prefs.setString("currentMask", "old mask")
        prefs.setInt("paradoxicity", 1)
        val lib = GameRuntimeLibrary(character = char, preferences = prefs)
        assertEquals("batmask", outputLib(lib, """print(my_mask());""").trim())
        assertEquals("15", outputLib(lib, """print(to_string(my_paradoxicity()));""").trim())
    }

    @Test
    fun myDiscomomentum_readsFromCharacterStateAfterFightSync() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """You're fighting <img src="discomo2.gif">""",
            "https://www.kingdomofloathing.com/fight.php",
        )
        assertEquals("2", outputLib(lib, """print(to_string(my_discomomentum()));""").trim())
    }

    @Test
    fun charpaneHook_updatesClassResources() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    path = AscensionPath.DISGUISES_DELIMIT.apiName,
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """Paradoxicity: 5<img src="masks/mask4.png">""",
            "https://www.kingdomofloathing.com/charpane.php",
        )
        assertEquals("batmask", outputLib(lib, """print(my_mask());""").trim())
        assertEquals("5", outputLib(lib, """print(to_string(my_paradoxicity()));""").trim())
    }

    @Test
    fun telescopeGetters_readFromCharacterState() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter().also { it.setCampground(telescopeUpgrades = 4, telescopeLookedHigh = true) }
        prefs.setInt("telescopeUpgrades", 1)
        prefs.setBoolean("telescopeLookedHigh", false)
        val lib = GameRuntimeLibrary(character = char, preferences = prefs)
        assertEquals("4", outputLib(lib, """print(to_string(telescope_upgrades()));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(telescope_looked_high()));""").trim())
    }

    @Test
    fun telescopeSync_mirrorsIntoCharacterState() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        TelescopeSync.parseResponse(
            "campground.php?action=telescopehigh",
            "<html>stars</html>",
            prefs,
            char,
        )
        assertEquals(true, char.state.value.telescopeLookedHigh)
        assertEquals("true", outputLib(GameRuntimeLibrary(character = char, preferences = prefs),
            """print(to_string(telescope_looked_high()));""").trim())
    }

    @Test
    fun revision_phase155() {
        assertEquals("phase230", GameRuntimeLibrary.REVISION)
    }
}
