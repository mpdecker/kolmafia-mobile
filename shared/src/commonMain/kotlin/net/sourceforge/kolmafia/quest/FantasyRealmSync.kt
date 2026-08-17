package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleFantasyRealmChange] + monorail `_frToday` from
 * [QuestManager.handleMonorailChange].
 */
object FantasyRealmSync {

    private val temporaryUnlocks = listOf(
        502 to "The Bandit Crossroads,",
        503 to "The Towering Mountains,",
        504 to "The Mystic Wood,",
        505 to "The Putrid Swamp,",
        506 to "The Cursed Village,",
        507 to "The Sprawling Cemetery,",
        509 to "The Old Rubee Mine,",
        510 to "The Foreboding Cave,",
        511 to "The Faerie Cyrkle,",
        512 to "The Druidic Campsite,",
        513 to "Near the Witch's House,",
        514 to "The Evil Cathedral,",
        515 to "The Barrow Mounds,",
        516 to "The Cursed Village Thieves' Guild,",
        517 to "The Troll Fortress,",
        518 to "The Labyrinthine Crypt,",
        519 to "The Lair of the Phoenix,",
        520 to "The Dragon's Moor,",
        521 to "Duke Vampire's Chateau,",
        522 to "The Master Thief's Chalet,",
        523 to "The Spider Queen's Lair,",
        524 to "The Archwizard's Tower,",
        525 to "The Ley Nexus,",
        526 to "The Ghoul King's Catacomb,",
        527 to "The Ogre Chieftain's Keep,",
    )

    fun applyFromFantasyPlace(url: String?, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (url != null &&
            !url.contains("whichplace=realm_fantasy", ignoreCase = true) &&
            !html.contains("whichplace=realm_fantasy", ignoreCase = true)
        ) {
            return false
        }
        if (!preferences.getBoolean("frAlways", false)) {
            preferences.setBoolean("_frToday", true)
        }
        if (html.contains("action=fr_initcenter")) {
            preferences.setBoolean("frMountainsUnlocked", html.contains("snarfblat=503"))
            preferences.setBoolean("frWoodUnlocked", html.contains("snarfblat=504"))
            preferences.setBoolean("frSwampUnlocked", html.contains("snarfblat=505"))
            preferences.setBoolean("frVillageUnlocked", html.contains("snarfblat=506"))
            preferences.setBoolean("frCemetaryUnlocked", html.contains("snarfblat=507"))
        } else {
            val unlocks = StringBuilder()
            for ((snarf, label) in temporaryUnlocks) {
                if (html.contains("snarfblat=$snarf")) unlocks.append(label)
            }
            preferences.setString("_frAreasUnlocked", unlocks.toString())
        }
        return true
    }

    fun applyFromMonorail(url: String?, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("whichplace=monorail", ignoreCase = true)) return false
        if (!html.contains("FantasyRealm")) return false
        if (preferences.getBoolean("frAlways", false)) return false
        preferences.setBoolean("_frToday", true)
        return true
    }
}
