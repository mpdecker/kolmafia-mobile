package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CrystalBallChoiceSync
import net.sourceforge.kolmafia.quest.FloristFriarChoiceSync
import net.sourceforge.kolmafia.quest.StillSuitChoiceSync
import net.sourceforge.kolmafia.quest.TrainsetChoiceSync
import net.sourceforge.kolmafia.request.ChateauRequest
import net.sourceforge.kolmafia.request.FloristRequest

class IotmManagerResidualMegaTest {

    private fun prefs() = Preferences(MapSettings())

    @AfterTest
    fun tearDown() {
        LocketManager.clear()
        CrystalBallManager.clear()
        FloristRequest.reset()
        ChateauRequest.reset()
        EncounterManager.ignoreSpecialMonsters = false
    }

    @Test
    fun locketParseMonstersAndFoughtCatalog() {
        val prefs = prefs()
        prefs.setString("_locketMonstersFought", "7")
        LocketManager.parseMonsters("""<option value="3"><option value="5">""", prefs)
        assertTrue(LocketManager.remembersMonster(3))
        assertTrue(LocketManager.remembersMonster(5))
        assertTrue(LocketManager.remembersMonster(7))
        assertTrue(LocketManager.foughtMonster(prefs, 7))
        assertFalse(LocketManager.foughtMonster(prefs, 3))
        assertTrue(LocketManager.isLocketFight("loverslocketframe.png"))
        assertTrue(LocketManager.own { if (it == ItemPool.COMBAT_LOVERS_LOCKET) 1 else 0 })
        assertTrue(LocketManager.onhand({ 0 }, equipped = true))
    }

    @Test
    fun stillSuitSweatAndDrink() {
        val prefs = prefs()
        assertTrue(
            StillSuitManager.handleSweat(
                """<img src="stillsuit.gif">""",
                prefs,
                familiarHasStillSuit = true,
                anyOwnedFamiliarHasStillSuit = false,
            ),
        )
        assertEquals(3, prefs.getInt("familiarSweat", 0))
        assertTrue(
            StillSuitManager.handleSweat(
                """<img src="stillsuit.gif">""",
                prefs,
                familiarHasStillSuit = false,
                anyOwnedFamiliarHasStillSuit = true,
            ),
        )
        assertEquals(4, prefs.getInt("familiarSweat", 0))
        assertTrue(
            StillSuitChoiceSync.apply(
                1476,
                1,
                """<b>11</b> drams<div>+10 Muscle</div>You put your lips to the nozzle""",
                prefs,
            ),
        )
        assertEquals(0, prefs.getInt("familiarSweat", 0))
        assertEquals("", prefs.getString("nextDistillateMods", "x"))
    }

    @Test
    fun crystalBallFightPredictionAndPonderPreserveTurn() {
        val prefs = prefs()
        assertTrue(
            CrystalBallManager.parseCrystalBall(
                html = "your next fight will be against <b>a spooky mummy</b>",
                locationName = "The Haunted Bedroom",
                currentRun = 10,
                preferences = prefs,
                findMonster = { if (it == "spooky mummy") it else null },
            ),
        )
        assertEquals(
            "10:The Haunted Bedroom:spooky mummy",
            prefs.getString(CrystalBallChoiceSync.PREDICTIONS_PREF, ""),
        )
        assertTrue(
            CrystalBallManager.isCrystalBallMonster("spooky mummy", "The Haunted Bedroom", equipped = true),
        )
        assertTrue(
            CrystalBallChoiceSync.applyVisit(
                1462,
                "<li> a spooky mummy in The Haunted Bedroom</li>",
                prefs,
                currentRun = 99,
                findLocation = { if (it == "The Haunted Bedroom") it else null },
                findMonster = { if (it == "spooky mummy") it else null },
            ),
        )
        assertEquals(
            "10:The Haunted Bedroom:spooky mummy",
            prefs.getString(CrystalBallChoiceSync.PREDICTIONS_PREF, ""),
        )
    }

    @Test
    fun juneCleaverColorFlashDecrementsFights() {
        val prefs = prefs()
        prefs.setInt("_juneCleaverFightsLeft", 3)
        assertTrue(
            JuneCleaverManager.updatePreferences(
                """As the battle ends, your cleaver flashes bright <span style="color: red" """,
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt("_juneCleaverFightsLeft", 0))
        assertEquals(1, prefs.getInt("_juneCleaverHot", 0))
    }

    @Test
    fun trainsetPieceApiAndMove() {
        val prefs = prefs()
        prefs.setString(
            "trainsetConfiguration",
            "empty,meat_mine,tower_fizzy,viewing_platform,tower_frozen,spooky_graveyard,logging_mill,candy_factory",
        )
        prefs.setInt("trainsetPosition", 0)
        val pieces = TrainsetManager.getTrainsetPieces(prefs)
        assertEquals(8, pieces.size)
        assertEquals(TrainsetChoiceSync.Piece.EMPTY_TRACK, pieces[0])
        assertTrue(TrainsetManager.onTrainsetMove("Meat Mine Sluice", prefs))
        assertEquals(1, prefs.getInt("trainsetPosition", 0))
    }

    @Test
    fun floristCatalogAndChateauFurniture() {
        val prefs = prefs()
        FloristFriarChoiceSync.reset()
        assertTrue(
            FloristFriarChoiceSync.apply(
                720,
                "choice.php?whichchoice=720&option=1&plant=7",
                "The Florist Friar's Cottage Ah, <b>The Sleazy Back Alley</b>!",
                prefs,
            ),
        )
        assertEquals(listOf(7), FloristFriarChoiceSync.plantsAt("The Sleazy Back Alley"))
        assertEquals("Deadly Cinnamon", FloristRequest.getPlants("The Sleazy Back Alley").single().plantName)
        assertTrue(FloristRequest.haveFlorist(prefs))

        ChateauRequest.parseFurniture(
            """<img src="nightstand_mus.gif"><img src="ceilingfan.gif"><img src="desk_bank.gif">""",
            prefs,
        )
        assertEquals(
            setOf(ItemPool.CHATEAU_MUSCLE, ItemPool.CHATEAU_FAN, ItemPool.CHATEAU_BANK),
            ChateauRequest.furnitureIds(),
        )
    }
}
