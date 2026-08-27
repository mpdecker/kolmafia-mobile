package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop CharSheetRequest.parseStatus subset (Phases 2271–2285).
 */
object CharSheetSync {
    private val USER_ID = Regex("""\(#(\d+)\)""")
    private val HP = Regex(
        """Hit Points:\s*([\d,]+)\s*/\s*([\d,]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val MP = Regex(
        """(?:Mana|Mojo|Muscularity|Psychic Energy) Points:\s*([\d,]+)\s*/\s*([\d,]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val MEAT = Regex("""Meat:\s*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val ADVENTURES = Regex("""Adventures Left:\s*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val ASCENSIONS = Regex("""Ascensions:\s*([\d,]+)""", RegexOption.IGNORE_CASE)
    private val INEBRIETY = Regex(
        """(?:Drunkenness|Tipsiness|Inebriety|Temulency):\s*([\d,]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val SIGN = Regex("""Sign:\s*([A-Za-z ]+)""", RegexOption.IGNORE_CASE)
    private val CLASS = Regex("""Class:\s*([A-Za-z ]+)""", RegexOption.IGNORE_CASE)

    fun parseStatus(
        html: String,
        character: KoLCharacter?,
        preferences: Preferences? = null,
    ): Boolean {
        if (character == null || html.isBlank()) return false
        if (html.contains("choice.php", ignoreCase = true) &&
            html.contains("whichchoice=", ignoreCase = true) &&
            !html.contains("Hit Points:", ignoreCase = true)
        ) {
            return false
        }

        var changed = false
        val plain = html.replace(Regex("<[^>]+>"), "\n")

        USER_ID.find(html)?.groupValues?.get(1)?.toIntOrNull()?.let { id ->
            character.setPlayerId(id)
            preferences?.setInt("playerId", id)
            changed = true
        }

        HP.find(plain)?.let { m ->
            val cur = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@let
            val max = m.groupValues[2].replace(",", "").toIntOrNull() ?: return@let
            val st = character.state.value
            character.updateHpMp(cur, max, st.currentMp, st.maxMp)
            changed = true
        }
        MP.find(plain)?.let { m ->
            val cur = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@let
            val max = m.groupValues[2].replace(",", "").toIntOrNull() ?: return@let
            val st = character.state.value
            character.updateHpMp(st.currentHp, st.maxHp, cur, max)
            changed = true
        }
        MEAT.find(plain)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let {
            character.updateMeat(it)
            changed = true
        }
        ADVENTURES.find(plain)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let { adv ->
            character.updateAdventuresLeft(adv)
            changed = true
        }
        INEBRIETY.find(plain)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let { drunk ->
            val st = character.state.value
            character.updateConsumables(st.fullness, drunk, st.spleenUsed)
            changed = true
        }
        SIGN.find(plain)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            character.setZodiacSign(it)
            changed = true
        }
        CLASS.find(plain)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { cls ->
            preferences?.setString("className", cls)
            changed = true
        }
        ASCENSIONS.find(plain)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let { asc ->
            preferences?.setInt("knownAscensions", asc)
            changed = true
        }

        preferences?.setBoolean(
            "hardcore",
            plain.contains("Hardcore", ignoreCase = true) &&
                !plain.contains("Not Hardcore", ignoreCase = true),
        )
        preferences?.setBoolean(
            "inRonin",
            plain.contains("Ronin", ignoreCase = true) &&
                !plain.contains("Out of Ronin", ignoreCase = true),
        )
        return changed
    }
}
