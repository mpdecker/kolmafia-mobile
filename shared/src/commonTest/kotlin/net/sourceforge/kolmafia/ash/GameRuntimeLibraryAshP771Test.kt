package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MummeryChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP771Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun costume_appliesModsAndUses() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MummeryChoiceSync.apply(
                choiceId = 1271,
                decision = 1,
                html = "You dress your familiar",
                preferences = prefs,
                familiarRace = "Mosquito",
                familiarHasAttribute = { it == "hashands" },
            ),
        )
        assertEquals("1,", prefs.getString("_mummeryUses", ""))
        assertTrue(prefs.getString("_mummeryMods", "").contains("Meat Drop: [30*fam(Mosquito)]"))
    }

    @Test
    fun costume_replacesSameFamiliar() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_mummeryMods", "Meat Drop: [15*fam(Mosquito)],")
        assertTrue(
            MummeryChoiceSync.apply(
                choiceId = 1271,
                decision = 4,
                html = "You dress your familiar",
                preferences = prefs,
                familiarRace = "Mosquito",
            ),
        )
        val mods = prefs.getString("_mummeryMods", "")
        assertTrue(mods.contains("Item Drop: [15*fam(Mosquito)]"))
        assertTrue(!mods.contains("Meat Drop"))
    }

    @Test
    fun questChoiceRules_wires1271() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1271,
                responseText = "You dress up",
                questDatabase = QuestDatabase(prefs),
                decision = 3,
                preferences = prefs,
                familiarRace = "Baby Gravy Fairy",
                familiarHasAttribute = { it == "animal" },
            ),
        )
        assertTrue(prefs.getString("_mummeryMods", "").contains("Experience (Muscle): [4*fam(Baby Gravy Fairy)]"))
    }
}
