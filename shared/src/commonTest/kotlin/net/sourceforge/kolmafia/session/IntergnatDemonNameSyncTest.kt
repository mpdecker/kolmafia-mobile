package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.familiar.FamiliarIds
import net.sourceforge.kolmafia.preferences.Preferences

class IntergnatDemonNameSyncTest {

    private fun sync(): IntergnatDemonNameSync =
        IntergnatDemonNameSync(Preferences(MapSettings()))

    @Test
    fun extractFragment_pattern1() {
        val html = """used to be a Ak'gyxoth but then I took an arrow to the knee"""
        val s = sync()
        s.updateFromFight(
            html,
            familiarId = FamiliarIds.INTERGNAT,
            randomModifiers = listOf("eldritch"),
        )
        assertEquals("Ak'gyxoth", s.demonName())
    }

    @Test
    fun extractFragment_pattern2() {
        val html = """All your base are belong to us"""
        val s = sync()
        s.updateFromFight(html, FamiliarIds.INTERGNAT, listOf("eldritch"))
        assertEquals("base", s.demonName())
    }

    @Test
    fun extractFragment_pattern3() {
        val html = """I'm a' chargin' mah lazer!" it shouts."""
        val s = sync()
        s.updateFromFight(html, FamiliarIds.INTERGNAT, listOf("eldritch"))
        assertEquals("lazer", s.demonName())
    }

    @Test
    fun extractFragment_pattern4() {
        val html = """I made you a cookie but I eated it!"""
        val s = sync()
        s.updateFromFight(html, FamiliarIds.INTERGNAT, listOf("eldritch"))
        assertEquals("cookie", s.demonName())
    }

    @Test
    fun twoStepMerge_contactThenName() {
        val prefs = Preferences(MapSettings())
        val s = IntergnatDemonNameSync(prefs)
        s.updateFromFight(
            "All your roa are belong to us",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        assertEquals("roa", s.demonName())
        s.updateFromFight(
            "used to be a Ak'gyxoth but then I took",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        assertEquals("Neil Ak'gyxoth roa", s.demonName())
    }

    @Test
    fun twoStepMerge_nameThenContact() {
        val prefs = Preferences(MapSettings())
        val s = IntergnatDemonNameSync(prefs)
        s.updateFromFight(
            "used to be a Ak'gyxoth but then I took",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        assertEquals("Ak'gyxoth", s.demonName())
        s.updateFromFight(
            "All your roa are belong to us",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        assertEquals("Neil Ak'gyxoth roa", s.demonName())
    }

    @Test
    fun neilPrefix_blocksFurtherUpdates() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.DEMON_NAME_12, "Neil Ak'gyxoth roa")
        val s = IntergnatDemonNameSync(prefs)
        s.updateFromFight(
            "All your other are belong to us",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        assertEquals("Neil Ak'gyxoth roa", s.demonName())
    }

    @Test
    fun duplicatePartType_ignored() {
        val prefs = Preferences(MapSettings())
        val s = IntergnatDemonNameSync(prefs)
        s.updateFromFight(
            "All your roa are belong to us",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        s.updateFromFight(
            "All your other are belong to us",
            FamiliarIds.INTERGNAT,
            listOf("eldritch"),
        )
        assertEquals("roa", s.demonName())
    }

    @Test
    fun nonIntergnat_noOp() {
        val s = sync()
        s.updateFromFight(
            "used to be a Ak'gyxoth but then I took",
            familiarId = 0,
            randomModifiers = listOf("eldritch"),
        )
        assertEquals("", s.demonName())
    }

    @Test
    fun noEldritchModifier_noOp() {
        val s = sync()
        s.updateFromFight(
            "used to be a Ak'gyxoth but then I took",
            FamiliarIds.INTERGNAT,
            randomModifiers = emptyList(),
        )
        assertEquals("", s.demonName())
    }
}
