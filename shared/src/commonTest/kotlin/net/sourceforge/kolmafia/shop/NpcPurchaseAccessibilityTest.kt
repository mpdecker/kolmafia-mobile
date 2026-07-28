package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED

class NpcPurchaseAccessibilityTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun store(key: String, name: String = key): NpcStoreData =
        NpcStoreData(storeKey = key, storeName = name, storeType = "NPC")

    @Test
    fun whiteCitadel_falseOnStep1() {
        val prefs = prefs()
        prefs.setString(Quest.CITADEL.prefKey, "step1")
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 623,
                store = store("whitecitadel", "White Citadel"),
                state = CharacterState(),
                prefs = prefs,
            ),
        )
    }

    @Test
    fun whiteCitadel_trueOnStep5() {
        val prefs = prefs()
        prefs.setString(Quest.CITADEL.prefKey, "step5")
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 623,
                store = store("whitecitadel", "White Citadel"),
                state = CharacterState(),
                prefs = prefs,
            ),
        )
    }

    @Test
    fun whiteCitadel_trueWhenFinished() {
        val prefs = prefs()
        prefs.setString(Quest.CITADEL.prefKey, FINISHED)
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 623,
                store = store("whitecitadel", "White Citadel"),
                state = CharacterState(),
                prefs = prefs,
            ),
        )
    }
}
