package net.sourceforge.kolmafia.mood

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectGainEffectIds
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EffectGainGateTest {

    private fun prefs(muffler: String = ""): Preferences {
        val settings = MapSettings()
        if (muffler.isNotEmpty()) {
            settings.putString(Preferences.PETE_MOTORBIKE_MUFFLER, muffler)
        }
        return Preferences(settings)
    }

    private fun effect(id: Int) = EffectData(id = id, name = "Effect $id", duration = 5)

    @Test fun cannotGainEffect_unknownEffect_returnsFalse() {
        assertFalse(
            EffectGainGate.cannotGainEffect(
                999,
                CharacterState(),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_silentHunting_nonSealClubber() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.SILENT_HUNTING,
                CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_nearlySilentHunting_sealClubber() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.NEARLY_SILENT_HUNTING,
                CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_boonOfSheWhoWas_wrongBlessingTrack() {
        val warBlessingState = EffectState(
            effects = listOf(effect(EffectGainEffectIds.BLESSING_OF_THE_WAR_SNAPPER)),
        )
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.BOON_OF_SHE_WHO_WAS,
                CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id),
                warBlessingState,
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_disdainOfSheWhoWas_turtleTamer() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.DISDAIN_OF_SHE_WHO_WAS,
                CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_shieldOfThePastalord_nonPastamancer() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.SHIELD_OF_THE_PASTALORD,
                CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_flimsyShield_pastamancer() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.FLIMSY_SHIELD_OF_THE_PASTALORD,
                CharacterState(characterClass = CharacterClass.PASTAMANCER.id),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_bloodyPotatoBits_nonPastamancer() {
        assertFalse(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.BLOODY_POTATO_BITS,
                CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id),
                EffectState(),
                prefs(),
            ),
        )
    }

    @Test fun cannotGainEffect_unmuffled_wrongMufflerPref() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.UNMUFFLED,
                CharacterState(),
                EffectState(),
                prefs("Extra-Quiet Muffler"),
            ),
        )
        assertFalse(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.UNMUFFLED,
                CharacterState(),
                EffectState(),
                prefs("Extra-Loud Muffler"),
            ),
        )
    }

    @Test fun cannotGainEffect_muffled_wrongMufflerPref() {
        assertTrue(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.MUFFLED,
                CharacterState(),
                EffectState(),
                prefs("Extra-Loud Muffler"),
            ),
        )
        assertFalse(
            EffectGainGate.cannotGainEffect(
                EffectGainEffectIds.MUFFLED,
                CharacterState(),
                EffectState(),
                prefs("Extra-Quiet Muffler"),
            ),
        )
    }
}
