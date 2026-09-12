package net.sourceforge.kolmafia.request

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop AccountRequest.parseAccountData / parseStatus (Phases 2271–2285 + 6071–6085).
 */
object AccountSync {
    private fun checkbox(flag: String, html: String): Boolean =
        html.contains("""checked="checked"  name="$flag"""") ||
            html.contains("""checked="checked" name="$flag"""")

    private fun flagInt(flags: JsonObject, key: String): Int {
        val el = flags[key] ?: return 0
        val prim = el as? JsonPrimitive ?: return 0
        return prim.contentOrNull?.toIntOrNull() ?: prim.intOrNull ?: 0
    }

    private fun rootInt(root: JsonObject, key: String): Int {
        val el = root[key] ?: return 0
        val prim = el as? JsonPrimitive ?: return 0
        return prim.contentOrNull?.toIntOrNull() ?: prim.intOrNull ?: 0
    }

    private fun rootString(root: JsonObject, key: String): String {
        val el = root[key] ?: return ""
        val prim = el as? JsonPrimitive ?: return ""
        return prim.contentOrNull.orEmpty()
    }

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
        val ignoreZone = checkbox("flag_ignorezonewarnings", html) ||
            checkbox("ignorezonewarnings", html)
        preferences.setBoolean("ignoreZoneWarnings", ignoreZone)
        character?.setIgnoreZoneWarnings(ignoreZone)
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
            character?.setSkillsRecalled(true)
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
                url.contains("Drop Hardcore", ignoreCase = true) ||
                url.contains("unhardcore", ignoreCase = true) -> {
                if (url.contains("unhardcoreconfirm=1", ignoreCase = true) ||
                    url.contains("confirm=1", ignoreCase = true) ||
                    html.contains("no longer Hardcore", ignoreCase = true)
                ) {
                    character?.setHardcore(false)
                    preferences.setBoolean("hardcore", false)
                }
            }
            url.contains("Drop+Bad+Moon", ignoreCase = true) ||
                url.contains("Drop Bad Moon", ignoreCase = true) -> {
                preferences.setBoolean("badMoon", false)
                character?.setZodiacSign("")
            }
            url.contains("Recall", ignoreCase = true) &&
                url.contains("Skills", ignoreCase = true) -> {
                preferences.setBoolean("skillsRecalled", true)
                character?.setSkillsRecalled(true)
            }
            url.contains("whichpenpal", ignoreCase = true) -> {
                val value = Regex("""(?:^|[?&])value=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (value != null) {
                    // Optimistic pref write from URL; desktop then ApiRequest.updateStatus().
                    applyEudora(value, preferences)
                    preferences.setBoolean("_eudoraNeedsStatusRefresh", true)
                }
            }
            url.contains("Forsake+Ronin", ignoreCase = true) ||
                url.contains("Forsake Ronin", ignoreCase = true) -> {
                if (url.contains("confirm=1", ignoreCase = true)) {
                    character?.setRoninLeft(0)
                    preferences.setInt("roninLeft", 0)
                }
            }
        }
        // Re-parse options after ajax toggle if body is a full tab
        if (html.contains("flag_wowbar", ignoreCase = true) ||
            html.contains("flag_ignorezonewarnings", ignoreCase = true)
        ) {
            parseOptionTab(html, preferences, character)
        }
    }

    /**
     * Desktop [AccountRequest.parseStatus] — flag_config + ascension fields from api.php status.
     */
    fun parseStatus(
        root: JsonObject,
        character: KoLCharacter?,
        preferences: Preferences?,
    ) {
        val flags = root["flag_config"]?.jsonObject
        if (flags != null) {
            val compact = flagInt(flags, "compactchar") == 1
            preferences?.setBoolean("compactCharacterPane", compact)

            val swapFam = flagInt(flags, "swapfam") == 1
            preferences?.setBoolean("swapFamiliarEquipment", swapFam)

            val ignoreZone = flagInt(flags, "ignorezonewarnings") == 1
            preferences?.setBoolean("ignoreZoneWarnings", ignoreZone)
            character?.setIgnoreZoneWarnings(ignoreZone)

            val sellUgly = flagInt(flags, "sellstuffugly") == 1
            preferences?.setBoolean("autosellUsesCompact", sellUgly)

            val lazy = flagInt(flags, "lazyinventory") == 1
            preferences?.setBoolean("lazyInventory", lazy)

            val unfam = flagInt(flags, "unfamequip") == 1
            preferences?.setBoolean("unequipFamiliarOnFight", unfam)

            val wowbar = flagInt(flags, "wowbar") == 1
            preferences?.setBoolean("serverAddsCustomCombat", wowbar)

            val autoAttack = flagInt(flags, "autoattack")
            character?.setAutoAttackAction(autoAttack)
            preferences?.setInt("defaultAutoAttack", autoAttack)

            applyEudora(flagInt(flags, "whichpenpal"), preferences)
            preferences?.setBoolean("_eudoraNeedsStatusRefresh", false)
        }

        val sign = rootString(root, "sign")
        if (sign.isNotBlank()) {
            character?.setZodiacSign(sign)
        }

        val pathId = rootInt(root, "path")
        if (pathId > 0 || root.containsKey("path")) {
            val path = AscensionPath.fromPathId(pathId)
            character?.setChallengePath(path.apiName)
        }

        val hardcore = rootInt(root, "hardcore") == 1 ||
            sign.equals("Bad Moon", ignoreCase = true)
        character?.setHardcore(hardcore)
        preferences?.setBoolean("hardcore", hardcore)

        val casual = rootInt(root, "casual") == 1
        character?.setCasual(casual)
        preferences?.setBoolean("casual", casual)

        val path = character?.state?.value?.ascensionPath
        if (path != AscensionPath.ACTUALLY_ED_THE_UNDYING) {
            val liberated = rootInt(root, "freedralph") == 1
            character?.setKingLiberated(liberated)
            preferences?.setBoolean("kingLiberated", liberated)
        } else {
            character?.setKingLiberated(false)
            preferences?.setBoolean("kingLiberated", false)
        }

        val recalled = rootInt(root, "recalledskills") == 1
        character?.setSkillsRecalled(recalled)
        preferences?.setBoolean("skillsRecalled", recalled)

        rootString(root, "pwd").takeIf { it.isNotBlank() }?.let { pwd ->
            preferences?.setString("pwdHash", pwd)
        }
    }

    private fun applyEudora(eudoraId: Int, preferences: Preferences?) {
        preferences ?: return
        val name = EUDORA_BY_ID[eudoraId] ?: "None"
        preferences.setString("currentEudora", name)
        preferences.setString("eudora", name)
    }

    private val EUDORA_BY_ID = mapOf(
        0 to "None",
        1 to "Pen Pal",
        2 to "GameInformPowerDailyPro Magazine",
        3 to "Xi Receiver Unit",
        4 to "New-You Club",
        5 to "Our Daily Candles",
        6 to "Black & White Apron",
    )
}
