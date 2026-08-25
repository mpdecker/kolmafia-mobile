package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest.updateFinalRoundData] familiarId switch (Phases 1521–1535)
 * for high-traffic charge / adventure / drop prefs.
 */
object FightFamiliarProgressSync {

    const val FAMILIAR_HARE = 50
    const val FAMILIAR_GIBBERER = 117
    const val FAMILIAR_HIPSTER = 136
    const val FAMILIAR_GRINDER = 139
    const val FAMILIAR_CANDLE = 210
    const val FAMILIAR_ROBORTENDER = 211
    const val FAMILIAR_GARBAGE_FIRE = 214
    const val FAMILIAR_CAT_BURGLAR = 267
    const val FAMILIAR_VAMPIRE_VINTNER = 284
    const val FAMILIAR_ROCKIN_ROBIN = 201
    const val FAMILIAR_PUCK_MAN = 230
    const val FAMILIAR_MS_PUCK_MAN = 231
    const val FAMILIAR_XO_SKELETON = 261
    const val FAMILIAR_MACHINE_ELF = 242
    const val FAMILIAR_RIFTLET = 82
    const val FAMILIAR_BOOTS = 98

    private val ROBORTENDER_DROP_MESSAGES = listOf(
        "Allow Me To Recommend A Local Specialty",
        "Perhaps You Would Enjoy A Drink Relevant To The Current Circumstances",
        "This Reminds Me Of A Classic Recipe",
        "Why Not Celebrate The Occasion With A Drink",
        "Why Not Try A Popular Local Recipe",
        "Fighting Works Up A Real Thirst",
        "Freshen Your Drink, Sir or Madam",
        "Have One For The Road",
        "I Hope I Am Not Enabling Any Addictions You Might Have",
        "It's Always Happy Hour Somewhere",
    )

    private const val DEEP_MACHINE_TUNNELS = 1105

    /**
     * Apply final-round familiar progress. Desktop runs this when combat finishes
     * (not saber/rollover runaway); call when [won] or [fightEnded].
     */
    fun apply(
        html: String,
        preferences: Preferences?,
        familiarId: Int = 0,
        won: Boolean = false,
        fightEnded: Boolean = false,
        underwater: Boolean = false,
        adventureId: Int = -1,
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        if (!won && !fightEnded) return false
        return when (familiarId) {
            FAMILIAR_HARE -> applyHare(html, preferences)
            FAMILIAR_GIBBERER -> applyGibberer(html, preferences, underwater)
            FAMILIAR_HIPSTER -> applyHipster(html, preferences)
            FAMILIAR_GRINDER -> applyGrinder(html, preferences)
            FAMILIAR_CANDLE -> {
                preferences.setInt(
                    "optimisticCandleProgress",
                    preferences.getInt("optimisticCandleProgress", 0) + 1,
                )
                true
            }
            FAMILIAR_GARBAGE_FIRE -> {
                preferences.setInt(
                    "garbageFireProgress",
                    preferences.getInt("garbageFireProgress", 0) + 1,
                )
                true
            }
            FAMILIAR_ROCKIN_ROBIN -> {
                preferences.setInt(
                    "rockinRobinProgress",
                    preferences.getInt("rockinRobinProgress", 0) + 1,
                )
                true
            }
            FAMILIAR_PUCK_MAN, FAMILIAR_MS_PUCK_MAN -> {
                preferences.setInt(
                    "powerPillProgress",
                    preferences.getInt("powerPillProgress", 0) + 1,
                )
                true
            }
            FAMILIAR_XO_SKELETON -> {
                preferences.setInt(
                    "xoSkeleltonXProgress",
                    preferences.getInt("xoSkeleltonXProgress", 0) + 1,
                )
                preferences.setInt(
                    "xoSkeleltonOProgress",
                    preferences.getInt("xoSkeleltonOProgress", 0) + 1,
                )
                true
            }
            FAMILIAR_ROBORTENDER -> applyRobortender(html, preferences)
            FAMILIAR_CAT_BURGLAR -> applyCatBurglar(html, preferences)
            FAMILIAR_VAMPIRE_VINTNER -> applyVintner(html, preferences)
            FAMILIAR_MACHINE_ELF -> applyMachineElf(html, preferences, adventureId)
            FAMILIAR_RIFTLET -> applyRiftlet(html, preferences)
            FAMILIAR_BOOTS -> applyBoots(html, preferences)
            else -> false
        }
    }

    fun applyHare(html: String, preferences: Preferences): Boolean {
        if (html.contains("oversized pocketwatch")) {
            preferences.setInt("_hareAdv", preferences.getInt("_hareAdv", 0) + 1)
            preferences.setInt("_hareCharge", 0)
        } else {
            preferences.setInt("_hareCharge", preferences.getInt("_hareCharge", 0) + 1)
        }
        return true
    }

    fun applyGibberer(
        html: String,
        preferences: Preferences,
        underwater: Boolean,
    ): Boolean {
        val step = if (underwater) 2 else 1
        var charge = preferences.getInt("_gibbererCharge", 0) + step
        if (charge > 15) charge = 15
        preferences.setInt("_gibbererCharge", charge)
        if (html.contains("you feel time slow down")) {
            preferences.setInt("_gibbererAdv", preferences.getInt("_gibbererAdv", 0) + 1)
            preferences.setInt(
                "_gibbererCharge",
                if (underwater) minOf(1, preferences.getInt("_gibbererCharge", 0)) else 0,
            )
        }
        return true
    }

    fun applyHipster(html: String, preferences: Preferences): Boolean {
        if (!html.contains("instantly grows a stupid-looking moustache")) return false
        preferences.setBoolean("_ironicMoustache", true)
        return true
    }

    fun applyGrinder(html: String, preferences: Preferences): Boolean {
        val stuffing = when {
            html.contains("some grinder fodder, muttering") -> "fish"
            html.contains("harvests a few choice bits for his grinder") -> "boss"
            html.contains("a few choice bits") -> "normal"
            html.contains("your opponent and tosses them") -> "stench"
            html.contains("insides, squealing something") -> "hot"
            html.contains("grind, chattering") -> "spooky"
            html.contains("My Hampton has a funny feeling") -> "sleaze"
            else -> return false
        }
        preferences.setInt("_piePartsCount", preferences.getInt("_piePartsCount", 0) + 1)
        val existing = preferences.getString("pieStuffing", "")
        val div = if (existing.isEmpty()) "" else ","
        if (preferences.getInt("_piePartsCount", 0) != 0) {
            preferences.setString("pieStuffing", existing + div + stuffing)
        }
        return true
    }

    fun applyRobortender(html: String, preferences: Preferences): Boolean {
        if (ROBORTENDER_DROP_MESSAGES.none { html.contains(it) }) return false
        preferences.setInt("_roboDrops", preferences.getInt("_roboDrops", 0) + 1)
        return true
    }

    fun applyCatBurglar(html: String, preferences: Preferences): Boolean {
        var changed = false
        if (html.contains("takes note of any security cameras in the area") ||
            html.contains("watches carefully to see if there are any guards and when they change shifts") ||
            html.contains("looks around for unlocked windows and accessible vents") ||
            html.contains("stands around casually, definitely just loitering and not casing the joint at all") ||
            html.contains("grabs a quick nap with his sleep mask, so he'll be fresh for the upcoming heist") ||
            html.contains("takes advantage of the downtime to grab a few z's") ||
            html.contains("disguises himself as someone who is asleep")
        ) {
            preferences.setInt(
                "_catBurglarCharge",
                preferences.getInt("_catBurglarCharge", 0) + 1,
            )
            changed = true
        }
        if (html.contains("Looks like he's ready for a heist") ||
            html.contains("cracks his knuckles and looks around for something to steal") ||
            html.contains("does some stretching exercises to prepare for his upcoming heist")
        ) {
            val charge = preferences.getInt("_catBurglarCharge", 0) + 1
            preferences.setInt("_catBurglarCharge", (charge / 10f).toInt() * 10)
            changed = true
        }
        return changed
    }

    fun applyVintner(html: String, preferences: Preferences): Boolean {
        if (html.contains("clears his throat") ||
            html.contains("gestures discreetly") ||
            html.contains("taps his foot")
        ) {
            preferences.setInt("vintnerCharge", 13)
            return true
        }
        val next = (preferences.getInt("vintnerCharge", 0) + 1).coerceAtMost(13)
        preferences.setInt("vintnerCharge", next)
        return true
    }

    fun applyMachineElf(
        html: String,
        preferences: Preferences,
        adventureId: Int,
    ): Boolean {
        if (html.contains("time starts passing again")) {
            val next = (preferences.getInt("_machineTunnelsAdv", 0) + 1).coerceAtMost(5)
            preferences.setInt("_machineTunnelsAdv", next)
            return true
        }
        if (adventureId == DEEP_MACHINE_TUNNELS) {
            preferences.setInt("_machineTunnelsAdv", 5)
            return true
        }
        return false
    }

    fun applyRiftlet(html: String, preferences: Preferences): Boolean {
        if (!html.contains("shimmers briefly, and you feel it getting earlier.")) return false
        preferences.setInt("_riftletAdv", preferences.getInt("_riftletAdv", 0) + 1)
        return true
    }

    fun applyBoots(html: String, preferences: Preferences): Boolean {
        if (html.contains("stomps your opponent into paste") ||
            html.contains("stomps your opponents into paste") ||
            html.contains("shuffles its heels, gets a running start, then leaps on")
        ) {
            preferences.setBoolean("bootsCharged", false)
            return true
        }
        return false
    }
}
