package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Parses Valhalla (Astral Spirit) charpane.php HTML when api.php status is unavailable (Phase 412). */
object CharpaneValhallaSync {

    @Volatile
    var inValhalla: Boolean = false
        private set

    private val karmaPatternCompact = Regex("""Karma:.*?<b>([^<]*)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val karmaPatternExpanded = Regex("""karma\.gif.*?<br>([^<]*)</td>""", RegexOption.DOT_MATCHES_ALL)

    fun reset() {
        inValhalla = false
    }

    fun isValhallaHtml(html: String, limitMode: String): Boolean {
        if (limitMode.isNotBlank() && !limitMode.equals("none", ignoreCase = true)) {
            return false
        }
        return html.contains("otherimages/spirit.gif") || html.contains("<br>Lvl. <img")
    }

    fun parseKarma(html: String, compact: Boolean): Int {
        val pattern = if (compact) karmaPatternCompact else karmaPatternExpanded
        val raw = pattern.find(html)?.groupValues?.getOrNull(1) ?: return 0
        return raw.replace(Regex("\\D+"), "").toIntOrNull() ?: 0
    }

    fun apply(
        character: KoLCharacter,
        html: String,
        preferences: Preferences?,
        effectManager: EffectManager?,
    ) {
        inValhalla = true
        val compact = CharpaneStatusSync.isCompact(html)
        val karma = parseKarma(html, compact)
        character.applyValhallaState()
        effectManager?.clearEffects()
        preferences?.setInt("bankedKarma", karma)
    }
}
