package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.EncounterData
import net.sourceforge.kolmafia.data.EncounterDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [EncounterManager] — encounter classification, session register, specials, wanderers
 * (Phases 1191–1220).
 */
object EncounterManager {
    data class RegisteredEncounter(
        val type: String?,
        val name: String,
        var count: Int = 1,
    ) {
        fun increment() {
            count++
        }
    }

    private val adventureList = mutableListOf<RegisteredEncounter>()
    private val encounterList = mutableListOf<RegisteredEncounter>()

    /** Desktop [EncounterManager.ignoreSpecialMonsters]. */
    var ignoreSpecialMonsters: Boolean = false

    /** Set when an autostop encounter fires and adventure loop should halt. */
    var pendingAutoStop: String? = null
        private set

    fun adventureListSnapshot(): List<RegisteredEncounter> = adventureList.toList()
    fun encounterListSnapshot(): List<RegisteredEncounter> = encounterList.toList()

    fun resetForTest() {
        adventureList.clear()
        encounterList.clear()
        ignoreSpecialMonsters = false
        pendingAutoStop = null
    }

    fun clearPendingAutoStop() {
        pendingAutoStop = null
    }

    fun ignoreSpecialMonsters() {
        ignoreSpecialMonsters = true
    }

    fun findEncounter(locationName: String?, encounterName: String): EncounterData? {
        val needle = encounterName.trim()
        if (needle.isEmpty()) return null
        val all = EncounterDatabase.all()
        return all.firstOrNull { e ->
            (locationName == null ||
                e.locationName == "*" ||
                e.locationName.equals(locationName, ignoreCase = true)) &&
                e.title.equals(needle, ignoreCase = true)
        }
    }

    fun findEncounter(encounterName: String): EncounterData? =
        findEncounter(null, encounterName)

    fun encounterType(encounterName: String): EncounterType {
        val found = findEncounter(encounterName)
        return found?.encounterType ?: EncounterType.NONE
    }

    fun isAutoStop(encounterName: String, preferences: Preferences? = null): Boolean {
        if (encounterName.equals("Under the Knife", ignoreCase = true) &&
            preferences?.getString("choiceAdventure21", "") == "2"
        ) {
            return false
        }
        return encounterType(encounterName).isAutostop
    }

    fun isWanderingMonster(encounter: String): Boolean =
        hasType(encounter, EncounterType.WANDERER)

    fun isLuckyMonster(encounter: String): Boolean =
        hasType(encounter, EncounterType.LUCKY)

    fun isSuperlikelyMonster(encounter: String): Boolean =
        hasType(encounter, EncounterType.SUPERLIKELY)

    fun isFreeCombatMonster(encounter: String): Boolean =
        hasType(encounter, EncounterType.FREE_COMBAT)

    fun isUltrarareMonster(encounter: String): Boolean =
        hasType(encounter, EncounterType.ULTRARARE)

    fun isNoWanderMonster(encounter: String): Boolean =
        hasType(encounter, EncounterType.NOWANDER)

    private fun hasType(encounter: String, type: EncounterType): Boolean =
        EncounterDatabase.all().any {
            it.title.equals(encounter, ignoreCase = true) && it.encounterType == type
        }

    fun registerAdventure(adventureName: String?) {
        if (adventureName.isNullOrBlank()) return
        val previous = adventureList.lastOrNull()
        if (previous != null && previous.name.equals(adventureName, ignoreCase = true)) {
            previous.increment()
        } else {
            adventureList.add(RegisteredEncounter(null, adventureName))
        }
    }

    fun registerEncounter(
        encounterName: String,
        encounterTypeLabel: String,
        responseText: String,
        preferences: Preferences? = null,
        effectManager: EffectManager? = null,
        locationName: String? = null,
        onAutoStop: ((String) -> Unit)? = null,
    ) {
        val name = encounterName.trim()
        if (name.isEmpty()) return

        handleSpecialEncounter(name, responseText, preferences)
        recognizeEncounter(name, responseText, preferences, effectManager, locationName, onAutoStop)

        val existing = encounterList.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (existing >= 0) {
            encounterList[existing].increment()
        } else {
            encounterList.add(RegisteredEncounter(encounterTypeLabel, name))
        }
    }

    private fun recognizeEncounter(
        encounterName: String,
        responseText: String,
        preferences: Preferences?,
        effectManager: EffectManager?,
        locationName: String?,
        onAutoStop: ((String) -> Unit)?,
    ) {
        val encounter = findEncounter(locationName, encounterName)
            ?: findEncounter(encounterName)
        val type = encounter?.encounterType ?: EncounterType.NONE
        if (type == EncounterType.NONE) return

        if (type == EncounterType.BADMOON) {
            BadMoonManager.registerAdventure(
                encounterName,
                preferences,
                preferences?.getInt("knownAscensions", 0) ?: 0,
            )
        }

        if (!type.isAutostop) return

        if (shouldSkipAutostop(effectManager)) return

        pendingAutoStop = encounterName
        onAutoStop?.invoke(encounterName)
        GoalManager.checkAutoStop(encounterName)
    }

    private fun shouldSkipAutostop(effectManager: EffectManager?): Boolean {
        val effects = effectManager?.state?.value?.effects.orEmpty()
        return effects.any {
            it.name.equals("Teleportitis", ignoreCase = true) ||
                it.name.equals("Feeling Lost", ignoreCase = true)
        }
    }

    // ── Wanderer / copier HTML detectors (Track B) ───────────────────────────

    fun isRomanticEncounter(responseText: String, checkMonster: Boolean, preferences: Preferences?): Boolean {
        if (responseText.contains("hear a wolf whistle") || responseText.contains("you feel the hairs")) {
            return true
        }
        if (!checkMonster || preferences == null) return false
        if (!preferences.getString("nextAdventure", "").equals("The Deep Machine Tunnels", ignoreCase = true)) {
            return false
        }
        val name = MonsterStatusTracker.getLastMonsterName()
        return name.equals(preferences.getString("romanticTarget", ""), ignoreCase = true)
    }

    fun isEnamorangEncounter(responseText: String, checkMonster: Boolean, preferences: Preferences?): Boolean {
        if (responseText.contains("tangled heartstrings")) return true
        if (!checkMonster || preferences == null) return false
        if (!preferences.getString("nextAdventure", "").equals("The Deep Machine Tunnels", ignoreCase = true)) {
            return false
        }
        val name = MonsterStatusTracker.getLastMonsterName()
        return name.equals(preferences.getString("enamorangMonster", ""), ignoreCase = true)
    }

    fun isDigitizedEncounter(responseText: String, checkMonster: Boolean, preferences: Preferences?): Boolean {
        if (responseText.contains("must have hit CTRL+V")) return true
        if (!checkMonster || preferences == null) return false
        if (!preferences.getString("nextAdventure", "").equals("The Deep Machine Tunnels", ignoreCase = true)) {
            return false
        }
        val name = MonsterStatusTracker.getLastMonsterName()
        return name.equals(preferences.getString("_sourceTerminalDigitizeMonster", ""), ignoreCase = true)
    }

    fun isSpookyVHSTapeMonster(responseText: String, checkMonster: Boolean, preferences: Preferences?): Boolean {
        if (responseText.contains("suddenly roll back and they fall... dead")) return true
        if (!checkMonster || preferences == null) return false
        if (!preferences.getString("nextAdventure", "").equals("The Deep Machine Tunnels", ignoreCase = true)) {
            return false
        }
        val name = MonsterStatusTracker.getLastMonsterName()
        return name.equals(preferences.getString("spookyVHSTapeMonster", ""), ignoreCase = true)
    }

    fun isGregariousEncounter(responseText: String): Boolean =
        responseText.contains("Looks like it's that friend you gregariously made")

    fun isMimeographEncounter(responseText: String): Boolean =
        responseText.contains("mimeo", ignoreCase = true)

    fun isSaberForceMonster(monsterName: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (preferences.getInt("_saberForceMonsterCount", 0) < 1) return false
        return monsterName.equals(preferences.getString("_saberForceMonster", ""), ignoreCase = true)
    }

    fun isSaberForceMonster(preferences: Preferences?): Boolean =
        isSaberForceMonster(MonsterStatusTracker.getLastMonsterName(), preferences)

    fun isRelativityMonster(preferences: Preferences?): Boolean {
        if (preferences?.getBoolean("_relativityMonster") != true) return false
        preferences.setBoolean("_relativityMonster", false)
        return true
    }

    fun isAfterimageMonster(preferences: Preferences?): Boolean {
        if (preferences?.getBoolean("_afterimageMonster") != true) return false
        preferences.setBoolean("_afterimageMonster", false)
        return true
    }

    /**
     * If fight HTML shows a copier/wanderer special, set [ignoreSpecialMonsters] like desktop FightRequest.
     */
    fun noteFightSpecials(responseText: String, preferences: Preferences?) {
        if (isRomanticEncounter(responseText, false, preferences) ||
            isEnamorangEncounter(responseText, false, preferences) ||
            isDigitizedEncounter(responseText, false, preferences) ||
            isSpookyVHSTapeMonster(responseText, false, preferences) ||
            isGregariousEncounter(responseText) ||
            isMimeographEncounter(responseText) ||
            isSaberForceMonster(preferences) ||
            isRelativityMonster(preferences) ||
            isAfterimageMonster(preferences)
        ) {
            ignoreSpecialMonsters()
        }
    }

    // ── handleSpecialEncounter high-traffic map (Track B) ────────────────────

    fun handleSpecialEncounter(
        encounterName: String,
        responseText: String,
        preferences: Preferences?,
    ) {
        val prefs = preferences ?: return
        when (encounterName.lowercase()) {
            "meat for nothing and the harem for free" ->
                prefs.setBoolean("_treasuryEliteMeatCollected", true)
            "finally, the payoff" ->
                prefs.setBoolean("_treasuryHaremMeatCollected", true)
            "faction traction = inaction" ->
                prefs.setInt("booPeakProgress", 98)
            "daily done, john." -> {
                prefs.setBoolean("dailyDungeonDone", true)
                prefs.setInt("_lastDailyDungeonRoom", 15)
            }
            "labrador conspirator" ->
                prefs.setInt("hallowienerCoinspiracy", prefs.getInt("hallowienerCoinspiracy", 0) + 1)
            "lava dogs" -> prefs.setBoolean("hallowienerVolcoino", true)
            "fruuuuuuuit" -> prefs.setBoolean("hallowienerSkeletonStore", true)
            "boooooze hound" -> prefs.setBoolean("hallowienerOvergrownLot", true)
            "baker's dogzen" -> prefs.setBoolean("hallowienerMadnessBakery", true)
            "dog needs food badly" ->
                prefs.setInt("hallowiener8BitRealm", prefs.getInt("hallowiener8BitRealm", 0) + 1)
            "ratchet-catcher" -> prefs.setBoolean("hallowienerMiddleChamber", true)
            "seeing-eyes dog" -> prefs.setBoolean("hallowienerDefiledNook", true)
            "carpenter dog" -> prefs.setBoolean("hallowienerSmutOrcs", true)
            "are they made of real dogs?" -> prefs.setBoolean("hallowienerGuanoJunction", true)
            "gunbowwowder" -> prefs.setBoolean("hallowienerSonofaBeach", true)
            "it isn't a poodle" -> prefs.setBoolean("hallowienerKnollGym", true)
            "you can never have enough" -> prefs.setBoolean("batWingsBatHoleEntrance", true)
            "bats of a feather" -> prefs.setBoolean("batWingsGuanoJunction", true)
            "one of us" -> prefs.setBoolean("batWingsBatratBurrow", true)
            "magical fruit" -> prefs.setBoolean("batWingsBeanbatChamber", true)
            else -> Unit
        }
    }
}
