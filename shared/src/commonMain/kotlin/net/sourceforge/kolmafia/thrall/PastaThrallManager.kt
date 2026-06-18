package net.sourceforge.kolmafia.thrall

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PastaThrall

/**
 * Pasta thrall state — preference-backed charpane sync for live [my_thrall].
 */
class PastaThrallManager(
    private val preferences: Preferences,
    private val character: KoLCharacter?,
) {

    fun currentThrallName(): String = preferences.getString(CURRENT_THRALL_PREF, "")

    fun isPastamancer(): Boolean =
        character?.state?.value?.characterClassEnum == CharacterClass.PASTAMANCER

    fun syncFromCharpane(html: String) {
        if (!isPastamancer()) return
        val parsed = PastaThrallCharpaneSync.parse(html)
        if (parsed == null) {
            preferences.setString(CURRENT_THRALL_PREF, "")
            return
        }
        val canonical = PastaThrall.canonicalType(parsed.type) ?: return
        val index = PastaThrall.typeIndex(canonical) ?: return
        PastaThrall.writePref(preferences, index, parsed.level, parsed.customName)
        preferences.setString(CURRENT_THRALL_PREF, canonical)
    }

    companion object {
        const val CURRENT_THRALL_PREF = "_currentThrall"
    }
}
