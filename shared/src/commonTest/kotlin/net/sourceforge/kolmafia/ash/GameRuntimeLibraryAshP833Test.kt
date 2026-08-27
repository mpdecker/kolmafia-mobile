package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LatteChoiceSync
import net.sourceforge.kolmafia.quest.LatteIngredients
import net.sourceforge.kolmafia.request.LatteRequest

class GameRuntimeLibraryAshP833Test {

    @Test
    fun unlockListings_filterKnownIngredients() {
        val prefs = Preferences(MapSettings())
        prefs.setString("latteUnlocks", "basil")
        val character = KoLCharacter().also {
            it.updateEquipment(EquipmentSlot.OFFHAND, LatteChoiceSync.LATTE_MUG_NAME)
        }
        val lib = GameRuntimeLibrary(character = character, preferences = prefs)
        val all = mutableListOf<String>()
        val unlocked = mutableListOf<String>()

        lib.cliLatte("unlocks", all::add)
        lib.cliLatte("unlocked", unlocked::add)

        assertTrue(all.any { it.startsWith("basil | unlocked") })
        assertTrue(all.any { it.startsWith("cajun | Unlock in The Black Forest") })
        assertTrue(unlocked.any { it.startsWith("basil | unlocked") })
        assertFalse(unlocked.any { it.startsWith("cajun |") })
    }

    @Test
    fun refillRejectsWrongIngredientCountBeforeHttp() {
        val output = mutableListOf<String>()
        val character = KoLCharacter().also {
            it.updateEquipment(EquipmentSlot.OFFHAND, LatteChoiceSync.LATTE_MUG_NAME)
        }
        GameRuntimeLibrary(character = character).cliLatte("refill basil", output::add)
        assertEquals(listOf("A latte refill requires exactly three ingredients."), output)
    }

    @Test
    fun parseChoiceOptions_extractsEachRadioSlot() {
        val html = """
            You've got <b>2</b> refills left.
            <tr style="x">
              <td><input type="radio" name="l1" value="11"> Basil and </td>
              <td><input type="radio" name="l2" checked value="22"> basil </td>
              <td><input type="radio" name="l3" value="33"> with basil </td>
            </tr>
        """.trimIndent()

        val options = LatteRequest.parseChoiceOptions(html, null)
        val basil = LatteIngredients.ALL.first { it.ingredient == "basil" }

        assertEquals(LatteRequest.RadioButtons("11", "22", "33"), options[basil])
    }
}
