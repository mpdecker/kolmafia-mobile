package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop [net.sourceforge.kolmafia.request.CharSheetRequest]. */
object CharSheetRequest {
    fun parseResponse(
        url: String,
        html: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        skillManager: SkillManager?,
    ) {
        if (!url.contains("charsheet.php", ignoreCase = true)) return
        CharSheetSync.parseStatus(html, character, preferences, skillManager)
    }

    fun registerRequest(url: String): Boolean = url.contains("charsheet.php", ignoreCase = true)
}
