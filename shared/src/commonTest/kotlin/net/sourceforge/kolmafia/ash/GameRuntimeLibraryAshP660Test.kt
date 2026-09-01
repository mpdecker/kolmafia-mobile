package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FireExtinguisherCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.SmutOrcCombatSync
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP660Test {

    @Test
    fun revision_phase665() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun foamEmUp_decrementsChargeByFive() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 100)
        val db = QuestDatabase(prefs)
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "The foam makes them both comical and immobile.",
                adventureId = "18",
                preferences = prefs,
                questDatabase = db,
            ),
        )
        assertEquals(95, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }

    @Test
    fun polarVortex_decrementsChargeByTen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 40)
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "You fire a blast of frigid extinguishant at your foe.",
                adventureId = "",
                preferences = prefs,
            ),
        )
        assertEquals(30, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }

    @Test
    fun batholeZone_advancesBatAndSpendsTwenty() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 80)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, "started")
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "You squeeze down the nozzle on your fire extinguisher and release a blast.",
                adventureId = "31",
                preferences = prefs,
                questDatabase = db,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.BAT))
        assertTrue(prefs.getBoolean("fireExtinguisherBatHoleUsed"))
        assertEquals(60, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }

    @Test
    fun batholeZone_doesNotAdvancePastStep2() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 100)
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.BAT, "step3")
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "You squeeze down the nozzle on your fire extinguisher and release a blast.",
                adventureId = "34",
                preferences = prefs,
                questDatabase = db,
            ),
        )
        assertEquals("step3", db.getProgress(Quest.BAT))
        assertTrue(prefs.getBoolean("fireExtinguisherBatHoleUsed"))
    }

    @Test
    fun cyrptZone_decreasesEvilnessByTen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 100)
        prefs.setInt("cyrptAlcoveEvilness", 50)
        prefs.setInt("cyrptTotalEvilness", 200)
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "The chill of the refrigerant quickly replaces some of the chill of evil in the air.",
                adventureId = CryptManager.DEFILED_ALCOVE.toString(),
                preferences = prefs,
            ),
        )
        assertEquals(40, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(190, prefs.getInt("cyrptTotalEvilness"))
        assertTrue(prefs.getBoolean("fireExtinguisherCyrptUsed"))
        assertEquals(80, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }

    @Test
    fun smutOrcZone_addsElevenProgressCappedAtFifteen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 50)
        prefs.setInt(SmutOrcCombatSync.PREF, 8)
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "You wantonly spray the area with your fire extinguisher.",
                adventureId = FireExtinguisherCombatSync.SMUT_ORC_ADVENTURE.toString(),
                preferences = prefs,
            ),
        )
        assertEquals(15, prefs.getInt(SmutOrcCombatSync.PREF))
        assertTrue(prefs.getBoolean("fireExtinguisherChasmUsed"))
    }

    @Test
    fun haremAndDesert_setUsedFlags() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 100)
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "You fill the harem with foam.",
                adventureId = FireExtinguisherCombatSync.HAREM_ADVENTURE.toString(),
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("fireExtinguisherHaremUsed"))
        assertTrue(
            FireExtinguisherCombatSync.apply(
                html = "You aim the nozzle directly into your mouth.",
                adventureId = FireExtinguisherCombatSync.DESERT_ADVENTURE.toString(),
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("fireExtinguisherDesertUsed"))
        assertEquals(60, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }

    @Test
    fun applyCombat_wiresChargeWithoutRequiringWin() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 25)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "Knob Goblin Harem Girl",
                won = false,
                preferences = prefs,
                adventureId = FireExtinguisherCombatSync.HAREM_ADVENTURE.toString(),
                responseText = "You fill the harem with foam.",
            ).advanced,
        )
        assertTrue(prefs.getBoolean("fireExtinguisherHaremUsed"))
        assertEquals(5, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }

    @Test
    fun withoutPhrases_isNoOp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(FireExtinguisherCombatSync.CHARGE_PREF, 100)
        assertFalse(
            FireExtinguisherCombatSync.apply(
                html = "You win the fight!",
                adventureId = "30",
                preferences = prefs,
            ),
        )
        assertEquals(100, prefs.getInt(FireExtinguisherCombatSync.CHARGE_PREF))
    }
}
