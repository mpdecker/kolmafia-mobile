package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LatteChoiceSync

class GameRuntimeLibraryAshP832Test {

    @Test
    fun fill_updatesPreferenceAndRuntimeItemModifiers() = runBlocking {
        GameDatabase().load()
        val prefs = Preferences(MapSettings())

        LatteChoiceSync.setLatteEnchantments(
            arrayOf("Meat Drop: 40", "Item Drop: 20", "Initiative: 50"),
            prefs,
        )

        assertEquals(
            "Meat Drop: 40, Item Drop: 20, Initiative: 50",
            prefs.getString("latteModifier", ""),
        )
        assertTrue(
            ModifierDatabase.getItem(LatteChoiceSync.LATTE_MUG_NAME)
                ?.modifiers
                ?.contains("Meat Drop: 40") == true,
        )
    }
}
