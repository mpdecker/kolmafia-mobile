package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemUsedSync
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP655Test {

    @Test
    fun revision_phase659() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    private val readingHtml = """
        <center>Total evil: <b>187</b><p>Alcove: <b>47</b><br>Cranny: <b>50</b><br>Niche: <b>40</b><br>Nook: <b>50</b></center>
    """.trimIndent()

    @Test
    fun examineEvilometer_parsesTotalAndCorners() {
        val prefs = Preferences(MapSettings())
        assertTrue(CryptManager.examineEvilometer(readingHtml, prefs))
        assertEquals(187, prefs.getInt("cyrptTotalEvilness"))
        assertEquals(47, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(50, prefs.getInt("cyrptCrannyEvilness"))
        assertEquals(40, prefs.getInt("cyrptNicheEvilness"))
        assertEquals(50, prefs.getInt("cyrptNookEvilness"))
    }

    @Test
    fun examineEvilometer_burialConsumesItem() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            CryptManager.examineEvilometer(
                "<center>Total evil: <b>0</b></center> You give it a proper burial.",
                prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(CryptManager.EVILOMETER to 1), consumed)
    }

    @Test
    fun questItemUsedSync_wiresEvilometer() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            QuestItemUsedSync.apply(
                CryptManager.EVILOMETER,
                readingHtml,
                db,
                prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(187, prefs.getInt("cyrptTotalEvilness"))
        assertTrue(consumed.isEmpty())
    }
}
