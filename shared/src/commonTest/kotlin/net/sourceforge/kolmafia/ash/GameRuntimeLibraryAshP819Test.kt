package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IceHouseChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP819Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_banishesPreservedMonster() {
        val prefs = Preferences(MapSettings())
        val banishes = BanishManager(prefs)
        assertTrue(
            IceHouseChoiceSync.applyVisit(
                choiceId = 836,
                html = "perfectly-preserved sabre-toothed lime, right?",
                banishManager = banishes,
                currentTurn = 10,
            ),
        )
        assertTrue(banishes.isBanished("sabre-toothed lime", 10))
        assertEquals(
            Banisher.ICE_HOUSE,
            banishes.getActiveBanishes(10)["sabre-toothed lime"],
        )
    }

    @Test
    fun post_decision1_removesIceHouseBanish() {
        val prefs = Preferences(MapSettings())
        val banishes = BanishManager(prefs)
        banishes.banishMonster("sabre-toothed lime", Banisher.ICE_HOUSE, 5)
        assertTrue(
            IceHouseChoiceSync.apply(
                choiceId = 836,
                decision = 1,
                banishManager = banishes,
            ),
        )
        assertFalse(banishes.isBanished("sabre-toothed lime", 10))
    }

    @Test
    fun questChoiceRules_wires836() {
        val prefs = Preferences(MapSettings())
        val banishes = BanishManager(prefs)
        banishes.banishMonster("foo", Banisher.ICE_HOUSE, 1)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 836,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                banishManager = banishes,
            ),
        )
        assertFalse(banishes.isBanished("foo", 1))
    }
}
