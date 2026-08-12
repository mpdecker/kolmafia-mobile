package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExoticBoostCliV5SupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun gap_findOption_aliases() {
        assertEquals(1, GapRequest.findOption("skill"))
        assertEquals(2, GapRequest.findOption("structure"))
        assertEquals(3, GapRequest.findOption("vision"))
        assertEquals(4, GapRequest.findOption("speed"))
        assertEquals(5, GapRequest.findOption("accuracy"))
        assertEquals(1, GapRequest.findOption("1"))
        assertEquals(0, GapRequest.findOption(""))
        assertEquals(0, GapRequest.findOption("strength"))
    }

    @Test
    fun gap_hasGapPantsEquipped() {
        val equipped = CharacterState(
            equipment = mapOf(EquipmentSlot.PANTS to "Greatest American Pants"),
        )
        assertTrue(GapRequest.hasGapPantsEquipped(equipped, "Greatest American Pants", null))
        assertFalse(
            GapRequest.hasGapPantsEquipped(
                CharacterState(equipment = mapOf(EquipmentSlot.PANTS to "trousers")),
                "Greatest American Pants",
                null,
            ),
        )
    }

    @Test
    fun spacegate_parseVaccine() {
        assertEquals(1, SpacegateRequest.parseVaccine("vaccine 1"))
        assertEquals(3, SpacegateRequest.parseVaccine("vaccine 3"))
        assertEquals(0, SpacegateRequest.parseVaccine("vaccine"))
        assertEquals(0, SpacegateRequest.parseVaccine("destination random"))
    }

    @Test
    fun daycare_findSpaOption() {
        assertEquals(1, DaycareRequest.findSpaOption("muscle"))
        assertEquals(2, DaycareRequest.findSpaOption("moxie"))
        assertEquals(3, DaycareRequest.findSpaOption("mysticality"))
        assertEquals(4, DaycareRequest.findSpaOption("regen"))
        assertEquals(0, DaycareRequest.findSpaOption("item"))
    }

    @Test
    fun vault3_form_and_preflight() {
        val form = FalloutShelterRequest.falloutShelterForm(FalloutShelterRequest.VAULT3)
        assertEquals("falloutshelter", form["whichplace"] ?: form.get("whichplace"))
        assertEquals("vault3", form["action"] ?: form.get("action"))

        assertNotNull(
            FalloutShelterRequest.preflightError(
                preferences = prefs(),
                inNuclearAutumn = false,
                limitMode = "",
            ),
        )
        assertNull(
            FalloutShelterRequest.preflightError(
                preferences = prefs {
                    putInt("falloutShelterLevel", 3)
                    putBoolean(FalloutShelterRequest.SPA_USED_PREF, false)
                },
                inNuclearAutumn = true,
                limitMode = "",
            ),
        )
        assertFalse(LimitModeGates.limitCampground(""))
    }

    @Test
    fun vault3_parseResponse_sets_pref() {
        val p = prefs()
        FalloutShelterRequest.parseVault3Response("You spend the entire day in the spa", p)
        assertTrue(p.getBoolean(FalloutShelterRequest.SPA_USED_PREF, false))
    }

    @Test
    fun grim_findOption() {
        assertEquals(1, GrimRequest.findOption("init"))
        assertEquals(1, GrimRequest.findOption("soles"))
        assertEquals(2, GrimRequest.findOption("hpmp"))
        assertEquals(2, GrimRequest.findOption("angry"))
        assertEquals(3, GrimRequest.findOption("damage"))
        assertEquals(3, GrimRequest.findOption("grumpy"))
        assertEquals(0, GrimRequest.findOption("buff"))
    }

    @Test
    fun aprilband_findEffectChoice() {
        assertEquals(1, AprilBandRequest.findEffectChoice("effect nc"))
        assertEquals(2, AprilBandRequest.findEffectChoice("effect c"))
        assertEquals(3, AprilBandRequest.findEffectChoice("effect drop"))
        assertEquals(1, AprilBandRequest.findEffectChoice("conduct noncombat"))
        assertEquals(0, AprilBandRequest.findEffectChoice("item sax"))
        assertEquals(0, AprilBandRequest.findEffectChoice("effect"))
    }

    @Test
    fun aprilband_hasHelmet() {
        assertTrue(
            AprilBandRequest.hasHelmet(
                charState = null,
                inventoryCounts = { if (it == AprilBandRequest.HELMET_ID) 1 else 0 },
                helmetName = null,
            ),
        )
        assertTrue(
            AprilBandRequest.hasHelmet(
                charState = CharacterState(
                    equipment = mapOf(EquipmentSlot.HAT to "Apriling band helmet"),
                ),
                inventoryCounts = { 0 },
                helmetName = "Apriling band helmet",
            ),
        )
        assertFalse(
            AprilBandRequest.hasHelmet(
                charState = CharacterState(),
                inventoryCounts = { 0 },
                helmetName = "Apriling band helmet",
            ),
        )
    }
}
