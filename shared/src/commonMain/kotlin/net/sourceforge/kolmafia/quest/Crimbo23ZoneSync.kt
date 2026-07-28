package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [PlaceRequest] crimbo23 place visit control pref sync. */
object Crimbo23ZoneSync {

    fun syncFromPlaceHtml(html: String, prefs: Preferences) {
        syncZone(html, "armory", "crimbo23ArmoryControl", "crimbo23ArmoryAtWar", prefs)
        syncZone(html, "bar", "crimbo23BarControl", "crimbo23BarAtWar", prefs)
        syncZone(html, "cafe", "crimbo23CafeControl", "crimbo23CafeAtWar", prefs)
        syncZone(html, "abuela", "crimbo23CottageControl", "crimbo23CottageAtWar", prefs)
        syncZone(html, "factory", "crimbo23FoundryControl", "crimbo23FoundryAtWar", prefs)
    }

    private fun syncZone(
        html: String,
        gifPrefix: String,
        controlPref: String,
        atWarPref: String,
        prefs: Preferences,
    ) {
        if (html.contains("${gifPrefix}_war.gif")) {
            prefs.setBoolean(atWarPref, true)
            prefs.setString(controlPref, "contested")
        } else {
            prefs.setBoolean(atWarPref, false)
            when {
                html.contains("${gifPrefix}_elf.gif") ->
                    prefs.setString(controlPref, "elf")
                html.contains("${gifPrefix}_pirate.gif") ->
                    prefs.setString(controlPref, "pirate")
                else ->
                    prefs.setString(controlPref, "none")
            }
        }
    }
}
