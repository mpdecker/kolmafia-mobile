package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FightItemPrefSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP683Test {

    @Test
    fun revision_phase683() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun flyer_incrementsMlFromAttack() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("flyeredML", 10)
        assertTrue(
            FightItemPrefSync.apply(
                html = "You slap a flyer up on your opponent. It enrages it.",
                monster = "war hippy",
                preferences = prefs,
                monsterAttack = { 25 },
            ),
        )
        assertEquals(35, prefs.getInt("flyeredML"))
    }

    @Test
    fun flyer_expiredConsumesStack() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FightItemPrefSync.apply(
                html = "Rock Promoters are long gone, and the scheduled day of the show has passed.",
                monster = "war hippy",
                preferences = prefs,
                combatItemId = FightItemPrefSync.JAM_BAND_FLYERS,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(FightItemPrefSync.JAM_BAND_FLYERS to 1))
    }

    @Test
    fun dna_setsPhylum() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FightItemPrefSync.apply(
                html = "you plunge the syringe into it and extract a sample of its DNA.",
                monster = "war hippy",
                preferences = prefs,
                combatItemId = FightItemPrefSync.DNA_SYRINGE,
                monsterPhylum = { "hippy" },
            ),
        )
        assertEquals("hippy", prefs.getString("dnaSyringe"))
    }

    @Test
    fun questFightRules_wiresFlyer() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "war hippy",
                won = false,
                preferences = prefs,
                responseText = "You slap a flyer up on your opponent.",
            ).advanced,
        )
        assertTrue(prefs.getInt("flyeredML") >= 0)
    }
}
