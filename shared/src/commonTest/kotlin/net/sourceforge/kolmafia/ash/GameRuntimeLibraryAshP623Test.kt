package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ProtonicGhostSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP623Test {

    @Test
    fun revision_phase623() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun quotedReport_setsLocation() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """&quot;Paranormal disturbance reported The Overgrown Lot.&quot;"""
        assertTrue(ProtonicGhostSync.parse(html, db, prefs, turnsPlayed = 100))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.GHOST))
        assertEquals("The Overgrown Lot", prefs.getString("ghostLocation"))
        assertEquals(151, prefs.getInt("nextParanormalActivity"))
    }

    @Test
    fun lastMatchWins() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """
            "Paranormal disturbance reported The Skeleton Store."
            "Paranormal disturbance reported The Haunted Kitchen."
        """.trimIndent()
        assertTrue(ProtonicGhostSync.parse(html, db, prefs, turnsPlayed = 10))
        assertEquals("The Haunted Kitchen", prefs.getString("ghostLocation"))
    }

    @Test
    fun fightWithoutPack_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """&quot;Paranormal disturbance reported The Spooky Forest.&quot;"""
        assertFalse(
            ProtonicGhostSync.applyFromFight(
                html = html,
                questDatabase = db,
                preferences = prefs,
                turnsPlayed = 0,
                equipment = emptyMap(),
                itemName = { "protonic accelerator pack" },
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.GHOST))
    }

    @Test
    fun fightWithPack_parses() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """&quot;Paranormal disturbance reported Inside the Palindome.&quot;"""
        assertTrue(
            ProtonicGhostSync.applyFromFight(
                html = html,
                questDatabase = db,
                preferences = prefs,
                turnsPlayed = 20,
                equipment = mapOf(EquipmentSlot.CONTAINER to "protonic accelerator pack"),
                itemName = { "protonic accelerator pack" },
            ),
        )
        assertEquals("Inside the Palindome", prefs.getString("ghostLocation"))
    }

    @Test
    fun crackleWithoutMatch_usesExistingLocation() {
        val prefs = Preferences(MapSettings())
        prefs.setString("ghostLocation", "The Icy Peak")
        val db = QuestDatabase(prefs)
        val html = "The walkie-talkie on your proton accelerator crackles to life"
        assertTrue(ProtonicGhostSync.parse(html, db, prefs, turnsPlayed = 5))
        assertEquals("The Icy Peak", prefs.getString("ghostLocation"))
        assertEquals(56, prefs.getInt("nextParanormalActivity"))
    }

    @Test
    fun walkieTalkie_parsesWithoutPack() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """&quot;Paranormal disturbance reported The Old Landfill.&quot;"""
        assertTrue(
            ProtonicGhostSync.applyFromWalkieTalkie(html, db, prefs, turnsPlayed = 0),
        )
        assertEquals("The Old Landfill", prefs.getString("ghostLocation"))
    }
}
