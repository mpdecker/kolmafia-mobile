package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest.handleLovebugs] + [FightRequest.maybeProcessLovebugsGain]
 * (Phases 1491–1505). Regex-first scan of fight HTML for `lb_*.gif` + nearby text.
 */
object FightLovebugSync {

    private val IMG = Regex(
        """(?:src|file)=["']?[^"'>]*(lb_[a-z]+\.gif)""",
        RegexOption.IGNORE_CASE,
    )
    private val MEAT_GAIN = Regex(
        """You gain ([\d,]+) Meat""",
        RegexOption.IGNORE_CASE,
    )
    private val MUSCLE_GAIN = Regex(
        """You gain ([\d,]+) (?:Fortitude|Muscle|Beefiness|Strength|Power)""",
        RegexOption.IGNORE_CASE,
    )
    private val MYST_GAIN = Regex(
        """You gain ([\d,]+) (?:Magicalness|Wizardliness|Enchantedness|Mysticality)""",
        RegexOption.IGNORE_CASE,
    )
    private val MOXIE_GAIN = Regex(
        """You gain ([\d,]+) (?:Cheek|Chutzpah|Roguishness|Moxie|Smarm)""",
        RegexOption.IGNORE_CASE,
    )

    fun apply(
        html: String,
        preferences: Preferences?,
        adventureId: String = "",
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        var changed = false
        if (html.contains("lb_") || html.contains("lovebug", ignoreCase = true) ||
            html.contains("love bug", ignoreCase = true)
        ) {
            if (!preferences.getBoolean("lovebugsUnlocked")) {
                preferences.setBoolean("lovebugsUnlocked", true)
                changed = true
            }
        }

        // Walk each lovebug image occurrence with a local text window
        for (match in IMG.findAll(html)) {
            val image = match.groupValues[1].lowercase()
            val start = (match.range.first - 80).coerceAtLeast(0)
            val end = (match.range.last + 400).coerceAtMost(html.length)
            val window = html.substring(start, end).replace(Regex("<[^>]+>"), " ")
            changed = handleImage(image, window, preferences, adventureId) || changed
        }

        // Also handle text-only high-traffic paths when images were stripped
        if (!IMG.containsMatchIn(html)) {
            changed = handleTextOnly(html, preferences) || changed
        }
        return changed
    }

    private fun handleTextOnly(html: String, preferences: Preferences): Boolean {
        var changed = false
        if (html.contains("love cricket") || html.contains("jaunty tune")) {
            increment(preferences, "lovebugsItemDrop")
            changed = true
        }
        if (html.contains("love grub shyly") || html.contains("extra Meat")) {
            // meat drop % bonus counter — distinct from lovebugsMeat
            if (html.contains("love grub")) {
                increment(preferences, "lovebugsMeatDrop")
                changed = true
            }
        }
        return changed
    }

    private fun handleImage(
        image: String,
        text: String,
        preferences: Preferences,
        adventureId: String,
    ): Boolean {
        when (image) {
            "lb_ant.gif" -> {
                increment(preferences, "lovebugsOrcChasm")
                return true
            }
            "lb_beetle.gif" -> {
                when {
                    text.contains("stag beetle") -> {
                        processDeferredGain(text, preferences, "lovebugsMuscle")
                        return true
                    }
                    text.contains("oil beetle") -> {
                        increment(preferences, "lovebugsOilPeak")
                        return true
                    }
                    text.contains("deathwatch beetle") -> {
                        increment(preferences, "lovebugsFreddy")
                        increment(preferences, "_lovebugsFreddy")
                        return true
                    }
                }
            }
            "lb_cicada.gif" -> {
                increment(preferences, "lovebugsChroner")
                increment(preferences, "_lovebugsChroner")
                return true
            }
            "lb_cricket.gif" -> {
                increment(preferences, "lovebugsItemDrop")
                return true
            }
            "lb_mosquito.gif", "lb_stink.gif", "lb_gnats.gif", "lb_scarab.gif" ->
                return true // combat skills — no fight-page prefs
            "lb_dragonfly.gif" -> {
                processDeferredGain(text, preferences, "lovebugsMoxie")
                return true
            }
            "lb_firefly.gif" -> {
                when {
                    text.contains("flits flirtatiously") -> {
                        processDeferredGain(text, preferences, "lovebugsMysticality")
                        return true
                    }
                    text.contains("seem slightly brighter") -> {
                        increment(preferences, "lovebugsCyrpt")
                        handleEvilometerLovebug(text, preferences, adventureId)
                        return true
                    }
                }
            }
            "lb_fly.gif" -> {
                increment(preferences, "lovebugsBooze")
                return true
            }
            "lb_grub.gif" -> {
                when {
                    text.contains("love grub") -> {
                        increment(preferences, "lovebugsMeatDrop")
                        return true
                    }
                    text.contains("love weevil") || text.contains("lovegrub") -> {
                        processDeferredGain(text, preferences, "lovebugsMeat")
                        return true
                    }
                }
            }
            "lb_roach.gif" -> {
                when {
                    text.contains("beneath a nearby stove") -> {
                        increment(preferences, "lovebugsPowder")
                        return true
                    }
                    text.contains("wadded-up bill") -> {
                        increment(preferences, "lovebugsFunFunds")
                        increment(preferences, "_lovebugsFunFunds")
                        return true
                    }
                }
            }
            "lb_spider.gif" -> {
                when {
                    text.contains("love water strider") -> {
                        increment(preferences, "lovebugsAridDesert")
                        return true
                    }
                    text.contains("drops off a coin") -> {
                        increment(preferences, "lovebugsCoinspiracy")
                        increment(preferences, "_lovebugsCoinspiracy")
                        return true
                    }
                }
            }
            "lb_tick.gif" -> {
                when {
                    text.contains("love scabie") || text.contains("love aphid") -> {
                        processDeferredGain(text, preferences, "lovebugsMeat")
                        return true
                    }
                    text.contains("love louse") -> {
                        increment(preferences, "lovebugsHoboNickel")
                        increment(preferences, "_lovebugsHoboNickel")
                        return true
                    }
                    text.contains("love snow flea") -> {
                        increment(preferences, "lovebugsWalmart")
                        increment(preferences, "_lovebugsWalmart")
                        return true
                    }
                }
            }
            "lb_worm.gif" -> {
                increment(preferences, "lovebugsBeachBuck")
                increment(preferences, "_lovebugsBeachBuck")
                return true
            }
        }
        return false
    }

    /** Desktop [FightRequest.maybeProcessLovebugsGain] — same-window scan. */
    fun processDeferredGain(
        text: String,
        preferences: Preferences,
        expectedProperty: String,
    ): Boolean {
        val amount = when (expectedProperty) {
            "lovebugsMeat" -> MEAT_GAIN.find(text)?.groupValues?.getOrNull(1)
            "lovebugsMuscle" -> MUSCLE_GAIN.find(text)?.groupValues?.getOrNull(1)
            "lovebugsMysticality" -> MYST_GAIN.find(text)?.groupValues?.getOrNull(1)
            "lovebugsMoxie" -> MOXIE_GAIN.find(text)?.groupValues?.getOrNull(1)
            else -> null
        }?.replace(",", "")?.toIntOrNull() ?: return false
        if (amount <= 0) return false
        increment(preferences, expectedProperty, amount)
        return true
    }

    fun handleEvilometerLovebug(
        text: String,
        preferences: Preferences,
        adventureId: String,
    ): Boolean {
        if (!text.contains("Evilometer beeps once")) return false
        // Prefer full Cyrpt fight evilness resolver (zone + gravy / vacuum extras).
        return CryptManager.handleFightEvilness(text, adventureId, preferences)
    }

    private fun increment(preferences: Preferences, key: String, delta: Int = 1) {
        preferences.setInt(key, preferences.getInt(key, 0) + delta)
    }
}
