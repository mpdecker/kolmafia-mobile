package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.campground.CampAwayAvailability
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExoticBoostCliV6SupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun terminal_resolveEnhanceCommand_aliases() {
        val p = prefs {
            putString("sourceTerminalEnhanceKnown", "substats.enh,damage.enh,critical.enh")
        }
        assertEquals("enhance items.enh", TerminalRequest.resolveEnhanceCommand("items", p))
        assertEquals("enhance init.enh", TerminalRequest.resolveEnhanceCommand("init.enh", p))
        assertEquals("enhance meat.enh", TerminalRequest.resolveEnhanceCommand("meat", p))
        assertEquals("enhance substats.enh", TerminalRequest.resolveEnhanceCommand("substats", p))
        assertEquals("enhance damage.enh", TerminalRequest.resolveEnhanceCommand("damage", p))
        assertEquals("enhance critical.enh", TerminalRequest.resolveEnhanceCommand("crit", p))
        assertNull(TerminalRequest.resolveEnhanceCommand("enquiry", p))
    }

    @Test
    fun terminal_enhanceLimit_cram_scram() {
        assertEquals(1, TerminalRequest.enhanceLimit(prefs()))
        assertEquals(
            2,
            TerminalRequest.enhanceLimit(prefs { putString("sourceTerminalChips", "CRAM") }),
        )
        assertEquals(
            3,
            TerminalRequest.enhanceLimit(
                prefs { putString("sourceTerminalChips", "CRAM,SCRAM") },
            ),
        )
    }

    @Test
    fun terminal_commandForm() {
        val form = TerminalRequest.terminalCommandForm("enhance meat.enh")
        assertEquals("1191", form["whichchoice"])
        assertEquals("1", form["option"])
        assertEquals("enhance meat.enh", form["input"])
    }

    @Test
    fun campaway_gate_and_form() {
        val form = CampAwayRequest.campAwayForm(CampAwayRequest.SKY)
        assertEquals("campaway", form["whichplace"])
        assertEquals("campaway_sky", form["action"])

        val locked = prefs()
        assertFalse(
            CampAwayAvailability.campAwayTentAvailable(CharacterState(), locked),
        )
        val unlocked = prefs { putBoolean("getawayCampsiteUnlocked", true) }
        assertTrue(
            CampAwayAvailability.campAwayTentAvailable(CharacterState(), unlocked),
        )

        val cloud = prefs()
        CampAwayRequest.parseCloudResponse("You get the Cloud-Talk effect", cloud)
        assertEquals(1, cloud.getInt(CampAwayRequest.CLOUD_BUFFS_PREF, 0))
    }

    @Test
    fun loathingidol_findStance() {
        assertEquals(1, LoathingIdolRequest.findStance("pop"))
        assertEquals(1, LoathingIdolRequest.findStance("moxie"))
        assertEquals(2, LoathingIdolRequest.findStance("ballad"))
        assertEquals(3, LoathingIdolRequest.findStance("rhyme"))
        assertEquals(4, LoathingIdolRequest.findStance("country"))
        assertEquals(0, LoathingIdolRequest.findStance(""))
        assertEquals(0, LoathingIdolRequest.findStance("dance"))
        assertEquals(11279, LoathingIdolRequest.findMicrophone { if (it == 11279) 1 else 0 })
        assertNull(LoathingIdolRequest.findMicrophone { 0 })
    }

    @Test
    fun mayam_parse_and_resolve() {
        assertEquals("caught yam-handed", MayamRequest.parseResonanceQuery("resonance caught yam-handed"))
        assertEquals("memories of cheesier age", MayamRequest.parseResonanceQuery("resonance memories of cheesier age"))
        assertNull(MayamRequest.parseResonanceQuery("resonance"))
        assertNull(MayamRequest.parseResonanceQuery(""))

        val p = prefs()
        assertEquals("caught yam-handed", MayamAvailability.resolveResonance("caught yam-handed", p))
        assertEquals("caught yam-handed", MayamAvailability.resolveResonance("yam-handed", p))
        assertEquals(
            listOf("chair", "yam", "yam", "clock"),
            MayamAvailability.symbolsFor("caught yam-handed"),
        )
        assertEquals(4, MayamAvailability.positionOnRing(0, "chair"))
        assertTrue(MayamAvailability.availableResonances(p).contains("caught yam-handed"))

        MayamAvailability.markSymbolsUsed(p, listOf("chair", "yam", "yam", "clock"))
        assertTrue(p.getString(MayamAvailability.SYMBOLS_USED_PREF, "").contains("chair"))
        assertTrue(p.getString(MayamAvailability.SYMBOLS_USED_PREF, "").contains("yam2"))
        assertTrue(p.getString(MayamAvailability.SYMBOLS_USED_PREF, "").contains("yam3"))
        assertFalse(MayamAvailability.availableResonances(p).contains("caught yam-handed"))
    }

    @Test
    fun asdon_drive_style_map() {
        assertEquals(0, AsdonMartinRequest.findDriveStyle("Obnoxiously"))
        assertEquals(1, AsdonMartinRequest.findDriveStyle("stealthily"))
        assertEquals(8, AsdonMartinRequest.findDriveStyle("Waterproofly"))
        assertEquals(-1, AsdonMartinRequest.findDriveStyle("fast"))
        assertEquals(5, AsdonMartinRequest.parseDriveStyle("drive quickly"))
        assertEquals(3, AsdonMartinRequest.parseDriveStyle("safely"))
        assertNotNull(AsdonMartinRequest.driveForm(1)["whichdrive"])
        assertEquals("drive", AsdonMartinRequest.driveForm(1)["preaction"])
        assertEquals("1", AsdonMartinRequest.driveForm(1)["whichdrive"])
    }
}
