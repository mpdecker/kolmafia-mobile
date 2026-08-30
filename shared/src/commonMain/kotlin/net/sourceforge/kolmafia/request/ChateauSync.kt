package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest chateau whichplace sync (Phases 2361–2375).
 */
object ChateauSync {
    private val FURNITURE = Regex(
        """title=["']([^"']+)["'][^>]*chateau""",
        RegexOption.IGNORE_CASE,
    )
    private val ALT_FURNITURE = Regex(
        """<b>(ceiling fan|fancy french bed|nightstand|armchair|desk|painting)[^<]*</b>""",
        RegexOption.IGNORE_CASE,
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        preferences?.setBoolean("chateauAvailable", true)
        parseFurniture(html, preferences)
        val action = PlaceSync.action(url)
        when {
            action.contains("rest") -> {
                preferences?.setInt("timesRested", (preferences.getInt("timesRested", 0) + 1))
                if (html.contains("free", ignoreCase = true)) {
                    preferences?.setInt(
                        "_freeRestsUsed",
                        preferences.getInt("_freeRestsUsed", 0) + 1,
                    )
                }
            }
            action.contains("painting") -> {
                preferences?.setBoolean("_chateauMonsterFought", true)
            }
            action.contains("desk") || html.contains("paperclip", ignoreCase = true) -> {
                if (url.contains("action=", ignoreCase = true)) {
                    preferences?.setBoolean("_chateauDeskHarvested", true)
                }
            }
        }
        ResultProcessor.processResults(false, html, null, character, preferences)
    }

    fun parseFurniture(html: String, preferences: Preferences?) {
        ChateauRequest.parseFurniture(html, preferences)
        preferences ?: return
        val found = linkedSetOf<String>()
        FURNITURE.findAll(html).forEach { found.add(it.groupValues[1].trim().lowercase()) }
        ALT_FURNITURE.findAll(html).forEach { found.add(it.groupValues[1].trim().lowercase()) }
        // Common chateau furniture prefs from desktop
        if (html.contains("ceiling fan", ignoreCase = true)) {
            preferences.setString("chateauCeiling", "ceiling fan")
        }
        if (html.contains("fancy french bed", ignoreCase = true) ||
            html.contains("electric muscle stimulator", ignoreCase = true) ||
            html.contains("continental breakfast buffet", ignoreCase = true)
        ) {
            preferences.setBoolean("chateauInstalled", true)
        }
        if (found.isNotEmpty()) {
            preferences.setString("_chateauFurniture", found.joinToString("|"))
        }
        if (html.contains("Painting of a", ignoreCase = true) ||
            html.contains("chateau painting", ignoreCase = true)
        ) {
            Regex(
                """Painting of a ([^.<]+)""",
                RegexOption.IGNORE_CASE,
            ).find(html)?.groupValues?.get(1)?.trim()?.let {
                preferences.setString("chateauMonsterName", it)
            }
        }
    }
}
