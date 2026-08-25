package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop AccountRequest.parseAccountData (Phases 2271–2285).
 */
object AccountSync {
    private fun checkbox(flag: String, html: String): Boolean =
        html.contains("""checked="checked"  name="$flag"""") ||
            html.contains("""checked="checked" name="$flag"""")

    fun parseAccountData(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        if (!url.contains("account.php", ignoreCase = true)) return
        if (url.contains("action=", ignoreCase = true)) {
            parseAction(url, html, preferences, character)
            return
        }
        parseOptionTab(html, preferences, character)
    }

    fun parseOptionTab(
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        preferences ?: return
        preferences.setBoolean("serverAddsCustomCombat", checkbox("flag_wowbar", html))
        preferences.setBoolean("serverAddsBothCombat", checkbox("flag_bothcombatinterf", html))
        preferences.setBoolean("lazyInventory", checkbox("flag_lazyinventory", html))
        preferences.setBoolean("unequipFamiliarOnFight", checkbox("flag_unfamequip", html))
        preferences.setBoolean("compactCharacterPane", checkbox("flag_compactchar", html))
        preferences.setBoolean("swapFamiliarEquipment", checkbox("flag_swapfam", html))
        // Autosell UI style
        preferences.setBoolean("autosellUsesCompact", !checkbox("flag_sellstuffugly", html))

        val autoAttack = Regex(
            """name=["']autoattack["'][^>]*>.*?selected[^>]*value=["'](\d+)["']""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex(
                """name=["']autoattack["'][^>]*>[\s\S]*?<option[^>]*value=["'](\d+)["'][^>]*selected""",
                RegexOption.IGNORE_CASE,
            ).find(html)?.groupValues?.get(1)?.toIntOrNull()
        if (autoAttack != null) {
            character?.setAutoAttackAction(autoAttack)
            preferences.setInt("defaultAutoAttack", autoAttack)
        }

        when {
            html.contains("""value="fancy"""", ignoreCase = true) &&
                html.contains("checked", ignoreCase = true) &&
                html.contains("fancy", ignoreCase = true) ->
                preferences.setString("topMenuStyle", "fancy")
            html.contains("compact", ignoreCase = true) &&
                checkbox("compact", html) ->
                preferences.setString("topMenuStyle", "compact")
        }

        if (html.contains("Drop Hardcore", ignoreCase = true)) {
            // Presence of button means still hardcore-capable UI; no clear.
        }
        if (!html.contains("Recall Skills", ignoreCase = true) &&
            preferences.getBoolean("kingLiberated", false)
        ) {
            preferences.setBoolean("skillsRecalled", true)
        }
    }

    fun parseAction(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter?,
    ) {
        preferences ?: return
        when {
            url.contains("Forsake", ignoreCase = true) ||
                url.contains("Drop+Hardcore", ignoreCase = true) ||
                url.contains("Drop Hardcore", ignoreCase = true) -> {
                character?.let {
                    val s = it.state.value
                    // Soft clear via meat/adventures untouched; hardcore flag via API preferred.
                }
                preferences.setBoolean("hardcore", false)
            }
            url.contains("Drop+Bad+Moon", ignoreCase = true) ||
                url.contains("Drop Bad Moon", ignoreCase = true) -> {
                preferences.setBoolean("badMoon", false)
            }
        }
        // Re-parse options after ajax toggle if body is a full tab
        if (html.contains("flag_wowbar", ignoreCase = true)) {
            parseOptionTab(html, preferences, character)
        }
    }
}
