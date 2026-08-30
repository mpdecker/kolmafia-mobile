package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [KoLAdventure.AdventureFailure] severity. */
enum class AdventureFailureSeverity { ERROR, PENDING }

/** Desktop [KoLAdventure.AdventureFailure] row. */
data class AdventureFailure(
    val responseText: String,
    val message: String,
    val severity: AdventureFailureSeverity = AdventureFailureSeverity.ERROR,
)

/**
 * Desktop [KoLAdventure] ADVENTURE_FAILURES table + findAdventureFailure
 * (Phases 2691–2720).
 */
object AdventureFailures {
    private val CRIMBO21_COLD_RES = Regex("""<b>\[(\d+) Cold Resistance Required]</b>""")

    val FAILURES: List<AdventureFailure> = listOf(
        AdventureFailure("", "KoL returned a blank page."),
        AdventureFailure("It is recommended that you have at least", "Your stats are too low for this location.  Adventure manually to acknowledge or disable this warning."),
        AdventureFailure("You shouldn't be here", "You can't get to that area."),
        AdventureFailure("not yet be accessible", "You can't get to that area."),
        AdventureFailure("You can't get there", "You can't get to that area."),
        AdventureFailure("Seriously.  It's locked.", "You can't get to that area."),
        AdventureFailure("You can't get to the 8-bit realm right now", "You can't get to that area."),
        AdventureFailure("You're out of adventures", "You're out of adventures.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You don't have any adventures.", "You're out of adventures.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You don't have enough Adventures left", "You're out of adventures.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You can't afford to go on a vacation", "You can't afford to go on a vacation."),
        AdventureFailure("You're too drunk to go on vacation", "You are too drunk to go on a vacation."),
        AdventureFailure("You're way too beaten up to go on an adventure right now", "You can't adventure at 0 HP.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Why go to the Tavern if you can't afford to drink?", "You can't afford to go out drinking."),
        AdventureFailure("You've already found the White Citadel", "The Road to the White Citadel is already cleared."),
        AdventureFailure("You don't appear to have all of the elements necessary to perform the ritual", "You don't have everything you need."),
        AdventureFailure("need some sort of stench protection", "You need stench protection."),
        AdventureFailure("You need some sort of protection from the cold", "You need cold protection."),
        AdventureFailure("I guess this particular information doesn't want to be free", "You need the Spookyraven library key."),
        AdventureFailure("You're too drunk to spelunk, as it were", "You are too drunk to go there."),
        AdventureFailure("You're too drunk to screw around", "You are too drunk to go there."),
        AdventureFailure("without some way of breathing underwater", "You can't breathe underwater."),
        AdventureFailure("wouldn't be able to breathe", "Your familiar can't breathe underwater."),
        AdventureFailure("You should consider a Mer-kin disguise.", "You aren't wearing a Mer-kin disguise."),
        AdventureFailure("The temporal rift in the plains has closed", "The temporal rift has closed.", AdventureFailureSeverity.PENDING),
        AdventureFailure("you are quickly identified as a stranger", "You aren't wearing an appropriate uniform."),
        AdventureFailure("Get into a uniform", "You aren't wearing an appropriate uniform."),
        AdventureFailure("There are no Frat soldiers left", "There are no Frat soldiers left."),
        AdventureFailure("There are no Hippy soldiers left", "There are no Hippy soldiers left."),
        AdventureFailure("You should probably stay out of there", "You have not been given the quest to go there yet.", AdventureFailureSeverity.PENDING),
        AdventureFailure("For some reason, you can't find your way back there", "You need to be Absinthe Minded to go there.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You break the bottle on the ground", "You are no longer gazing into the bottle.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You're in the regular dimension now", "You are no longer Half-Astral.", AdventureFailureSeverity.PENDING),
        AdventureFailure("faded back into the spectral mists", "No one may know of its coming or going."),
        AdventureFailure("can't find any additional ducks", "Nothing more to do here today.", AdventureFailureSeverity.PENDING),
        AdventureFailure("no more ducks here", "Farm area cleared.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You don't know where that place is.", "Use a \\\"DRINK ME\\\" potion before trying to adventure here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Looks like they don't let anything in here if they don't recognize its smell.", "Use a filthworm hatchling scent gland before trying to adventure here."),
        AdventureFailure("Looks like the guards will only let you in here if you smell like food.", "Use a filthworm drone scent gland before trying to adventure here."),
        AdventureFailure("You must not smell right to 'em.", "Use a filthworm royal guard scent gland before trying to adventure here."),
        AdventureFailure("The filthworm queen has been slain", "The filthworm queen has been slain."),
        AdventureFailure("already retrieved all of the stolen Meat", "You already recovered the Nuns' Meat.", AdventureFailureSeverity.PENDING),
        AdventureFailure("the way to their camp is clear", "There are no hippy soldiers left.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You've already slain the Goblin King", "You already defeated the Goblin King.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Bonerdagon has been defeated", "You already defeated the Bonerdagon.", AdventureFailureSeverity.PENDING),
        AdventureFailure("already undefiled", "Cyrpt area cleared.", AdventureFailureSeverity.PENDING),
        AdventureFailure("otherworldly whispers", "You already defeated Lord Spookyraven.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Ed the Undying sleeps once again", "Ed the Undying has already been defeated.", AdventureFailureSeverity.PENDING),
        AdventureFailure("don't trust those rats not to steal", "You don't trust those rats not to steal your token!.", AdventureFailureSeverity.PENDING),
        AdventureFailure("That's too far to walk", "The Carriageman isn't drunk enough to take you there.", AdventureFailureSeverity.PENDING),
        AdventureFailure("The forest is silent", "The Dreadsylvanian Woods boss has been defeated.", AdventureFailureSeverity.PENDING),
        AdventureFailure("The village is now a ghost town", "The Dreadsylvanian Village boss has been defeated.", AdventureFailureSeverity.PENDING),
        AdventureFailure("the king is dead, baby", "The Dreadsylvanian Castle boss has been defeated.", AdventureFailureSeverity.PENDING),
        AdventureFailure("cleared that ancient protector spirit out", "You already defeated the protector spirit in that square."),
        AdventureFailure("the altar doesn't really do anything but look neat", "You already used the altar in that square."),
        AdventureFailure("lying here in a pile just where you left him", "You already looted Dr. Fanning in that square."),
        AdventureFailure("You wander into the empty temple", "You already looted the temple in that square."),
        AdventureFailure("You'll have to find another way up", "You haven't opened the ground floor of the castle yet."),
        AdventureFailure("you can't get to the ground floor", "You haven't opened the ground floor of the castle yet."),
        AdventureFailure("You'll have to figure out some other way to get upstairs", "You haven't opened the top floor of the castle yet."),
        AdventureFailure("prepare to save some children", "The portal is open."),
        AdventureFailure("things are running pretty smoothly", "Nothing more to do here today."),
        AdventureFailure("You should talk to Edwing", "Nothing more to do here today."),
        AdventureFailure("The compound is abandoned now", "Nothing more to do here today."),
        AdventureFailure("some way of escaping gravity", "You are not wearing a warbear hoverbelt."),
        AdventureFailure("it's out of juice", "Your hoverbelt needs a new battery."),
        AdventureFailure("you don't have a code", "You don't have a warbear badge."),
        AdventureFailure("There's nothing left of Ol' Scratch", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("There's nothing left in Exposure Esplanade", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("The Heap is empty", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("There's nothing going on here anymore", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("There's nothing left in the Purple Light District", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Hobopolis Town Square lies empty", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("bathrooms are empty now", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("there's no way you're going all the way through that slash", "You don't have a flying mount.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You can't do anything without some way of flying", "You don't have a flying mount.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You should  maybe come back when you're at least slightly less drunk", "You are too drunk.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You don't have the energy to attack a problem this size", "You need at least 20% buffed max MP.", AdventureFailureSeverity.PENDING),
        AdventureFailure("You're not in good enough shape to deal with a threat this large", "You need at least 20% buffed max HP.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Your El Vibrato portal has run out of power", "Your El Vibrato portal has run out of power", AdventureFailureSeverity.PENDING),
        AdventureFailure("you don't know the transporter frequency", "You are no longer Transpondent."),
        AdventureFailure("without the proper transporter frequency", "You are no longer Transpondent."),
        AdventureFailure("you can't visit the Suburbs of Dis", "You are no longer Dis Abled."),
        AdventureFailure("area around the portal is quiet", "The Abyssal Portal is quiet."),
        AdventureFailure("Who knows what would happen if you breathed the air", "You need to equip your Personal Ventilation Unit."),
        AdventureFailure("You already cleared out this area", "You already cleared out this area"),
        AdventureFailure("This area is closed", "You completed the video game"),
        AdventureFailure("off the Florida Keys", "You need a Tropical Contact High to go there."),
        AdventureFailure("only open for gravy fairies", "You need to bring an elemental gravy fairy with you."),
        AdventureFailure("You shouldn't be here dressed like that", "You can't pass as a pirate."),
        AdventureFailure("LOLmec's lair lies lempty", "You already beat LOLmec"),
        AdventureFailure("Already beat Yomama", "You already beat Yomama"),
        AdventureFailure("The ghost has arrived", "Your tale of spelunking is over."),
        AdventureFailure("The gingerbread city has collapsed.", "The gingerbread city has collapsed."),
        AdventureFailure("you can't get to it", "You need a way to travel in space."),
        AdventureFailure("out of energy for today", "The Spacegate is out of energy for today."),
        AdventureFailure("you can't bear the silence", "You are not wearing your anti-earplugs.."),
        AdventureFailure("redeploy more mimes to it by tomorrow", "There are no more mimes left today."),
        AdventureFailure("your time in FantasyRealm has come to an end for today", "Your time in FantasyRealm is over for today."),
        AdventureFailure("It'll probably get rowdy again by tomorrow", "The Neverending Party is over for today."),
        AdventureFailure("without your PARTY HARD shirt on", "Cannot adventure at The Neverending Party without your PARTY HARD shirt on."),
        AdventureFailure("That isn't a place you can get to the way you're dressed", "You're not equipped properly to adventure there."),
        AdventureFailure("Equip those boots you found", "Plumbers cannot adventure without appropriate gear."),
        AdventureFailure("going to take a <i>long</i> time", "You need 7 Adventures to fight Ed."),
        AdventureFailure("Drippy Juice supply", "You've run out of Drippy Juice."),
        AdventureFailure("You don't know where that is", "You can't get there from here."),
        AdventureFailure("That isn't a place you can go", "You can't get there from here."),
        AdventureFailure("You can't go there right now", "You're not allowed to go there."),
        AdventureFailure("Better bundle up", "You need more cold resistance."),
        AdventureFailure("extreme cold makes it impossible", "You need more cold resistance."),
        AdventureFailure("This zone is too old to visit on this path.", "That zone is out of Standard."),
        AdventureFailure("you've gotta be somebody special", "You're not allowed to go there."),
        AdventureFailure("they're not gonna let you in dressed like this", "You're not dressed appropriately."),
        AdventureFailure("The temple is empty", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("you've gotta be especially gladitorial", "You need to wear the Mer-kin Gladiatorial Gear."),
        AdventureFailure("you've gotta be somebody especially pious", "You need to wear the Mer-kin Scholar's Vestments."),
        AdventureFailure("gesture pointedly with their eyefins at the doors to your left and right", "You must defeat the Elder Gods of Hatred and Violence first."),
        AdventureFailure("You've already defeated the Trainbot boss.", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("Looks like peace has broken out in this area", "The balance of power has shifted, you can no longer fight here", AdventureFailureSeverity.PENDING),
        AdventureFailure("You've already hacked this system.", "Nothing more to do here.", AdventureFailureSeverity.PENDING),
        AdventureFailure("If you want Axis HQ access codes", "You do not have the Current Axis Codes.")
    )

    fun findAdventureFailure(responseText: String, preferences: Preferences? = null): Int {
        if (responseText.isEmpty()) return 0
        applySideEffects(responseText, preferences)
        for (i in 1 until FAILURES.size) {
            if (responseText.contains(FAILURES[i].responseText)) return i
        }
        return -1
    }

    fun adventureFailureMessage(index: Int): String? =
        FAILURES.getOrNull(index)?.message

    fun adventureFailureSeverity(index: Int): AdventureFailureSeverity =
        FAILURES.getOrNull(index)?.severity ?: AdventureFailureSeverity.ERROR

    fun toStopReason(index: Int): StopReason? {
        if (index < 0) return null
        val message = adventureFailureMessage(index) ?: return null
        return when (adventureFailureSeverity(index)) {
            AdventureFailureSeverity.PENDING -> when {
                message.contains("out of adventures", ignoreCase = true) -> StopReason.NoAdventuresLeft
                message.contains("0 HP", ignoreCase = true) -> StopReason.CharacterDeath
                else -> StopReason.AdventureFailure(message, pending = true)
            }
            AdventureFailureSeverity.ERROR -> StopReason.AdventureFailure(message, pending = false)
        }
    }

    private fun applySideEffects(responseText: String, preferences: Preferences?) {
        val prefs = preferences ?: return
        when {
            responseText.contains("There are no Hippy soldiers left") ->
                prefs.setInt("hippiesDefeated", 1000)
            responseText.contains("There are no Frat soldiers left") ->
                prefs.setInt("fratboysDefeated", 1000)
            responseText.contains("Drippy Juice supply") ->
                prefs.setInt("drippyJuice", 0)
            responseText.contains("El Vibrato portal") ->
                prefs.setInt("currentPortalEnergy", 0)
            responseText.contains("spacegate is out of energy") ->
                prefs.setInt("_spacegateTurnsLeft", 0)
            responseText.contains("Better bundle up") ||
                responseText.contains("extreme cold makes it impossible") -> {
                val required = CRIMBO21_COLD_RES.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
                if (required != null) prefs.setInt("_crimbo21ColdResistance", required)
            }
            responseText.contains("Looks like peace has broken out in this area.") ->
                prefs.setBoolean("_crimbo23PeaceRefreshNeeded", true)
        }
    }
}

